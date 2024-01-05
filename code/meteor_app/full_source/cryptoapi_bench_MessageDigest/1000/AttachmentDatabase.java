/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaDataSource;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.bumptech.glide.Glide;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.json.JSONArray;
import org.json.JSONException;
import org.signal.core.util.StreamUtil;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.attachments.AttachmentId;
import org.thoughtcrime.securesms.attachments.DatabaseAttachment;
import org.thoughtcrime.securesms.audio.AudioHash;
import org.thoughtcrime.securesms.blurhash.BlurHash;
import org.thoughtcrime.securesms.crypto.AttachmentSecret;
import org.thoughtcrime.securesms.crypto.ClassicDecryptingPartInputStream;
import org.thoughtcrime.securesms.crypto.ModernDecryptingPartInputStream;
import org.thoughtcrime.securesms.crypto.ModernEncryptingPartOutputStream;
import org.thoughtcrime.securesms.database.helpers.SQLCipherOpenHelper;
import org.thoughtcrime.securesms.database.model.databaseprotos.AudioWaveFormData;
import org.thoughtcrime.securesms.mms.MediaStream;
import org.thoughtcrime.securesms.mms.MmsException;
import org.thoughtcrime.securesms.mms.PartAuthority;
import org.thoughtcrime.securesms.mms.SentMediaQuality;
import org.thoughtcrime.securesms.stickers.StickerLocator;
import org.thoughtcrime.securesms.util.Base64;
import org.thoughtcrime.securesms.util.CursorUtil;
import org.thoughtcrime.securesms.util.FileUtils;
import org.thoughtcrime.securesms.util.JsonUtils;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.SetUtil;
import org.thoughtcrime.securesms.util.SqlUtil;
import org.thoughtcrime.securesms.util.StorageUtil;
import org.thoughtcrime.securesms.video.EncryptedMediaDataSource;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.internal.util.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AttachmentDatabase extends Database {
  
  private static final String TAG = Log.tag(AttachmentDatabase.class);

  public  static final String TABLE_NAME             = "part";
  public  static final String ROW_ID                 = "_id";
          static final String ATTACHMENT_JSON_ALIAS  = "attachment_json";
  public  static final String MMS_ID                 = "mid";
          static final String CONTENT_TYPE           = "ct";
          static final String NAME                   = "name";
          static final String CONTENT_DISPOSITION    = "cd";
          static final String CONTENT_LOCATION       = "cl";
  public  static final String DATA                   = "_data";
          static final String TRANSFER_STATE         = "pending_push";
  private static final String TRANSFER_FILE          = "transfer_file";
  public  static final String SIZE                   = "data_size";
          static final String FILE_NAME              = "file_name";
  public  static final String UNIQUE_ID              = "unique_id";
          static final String DIGEST                 = "digest";
          static final String VOICE_NOTE             = "voice_note";
          static final String BORDERLESS             = "borderless";
          static final String VIDEO_GIF              = "video_gif";
          static final String QUOTE                  = "quote";
  public  static final String STICKER_PACK_ID        = "sticker_pack_id";
  public  static final String STICKER_PACK_KEY       = "sticker_pack_key";
          static final String STICKER_ID             = "sticker_id";
          static final String STICKER_EMOJI          = "sticker_emoji";
          static final String FAST_PREFLIGHT_ID      = "fast_preflight_id";
  public  static final String DATA_RANDOM            = "data_random";
          static final String WIDTH                  = "width";
          static final String HEIGHT                 = "height";
          static final String CAPTION                = "caption";
          static final String DATA_HASH              = "data_hash";
          static final String VISUAL_HASH            = "blur_hash";
          static final String TRANSFORM_PROPERTIES   = "transform_properties";
          static final String DISPLAY_ORDER          = "display_order";
          static final String UPLOAD_TIMESTAMP       = "upload_timestamp";
          static final String CDN_NUMBER             = "cdn_number";

  public  static final String DIRECTORY              = "parts";

  public static final int TRANSFER_PROGRESS_DONE    = 0;
  public static final int TRANSFER_PROGRESS_STARTED = 1;
  public static final int TRANSFER_PROGRESS_PENDING = 2;
  public static final int TRANSFER_PROGRESS_FAILED  = 3;

  public static final long PREUPLOAD_MESSAGE_ID = -8675309;

  private static final String PART_ID_WHERE     = ROW_ID + " = ? AND " + UNIQUE_ID + " = ?";
  private static final String PART_ID_WHERE_NOT = ROW_ID + " != ? AND " + UNIQUE_ID + " != ?";

  private static final String[] PROJECTION = new String[] {ROW_ID,
                                                           MMS_ID, CONTENT_TYPE, NAME, CONTENT_DISPOSITION,
                                                           CDN_NUMBER, CONTENT_LOCATION, DATA,
                                                           TRANSFER_STATE, SIZE, FILE_NAME, UNIQUE_ID, DIGEST,
                                                           FAST_PREFLIGHT_ID, VOICE_NOTE, BORDERLESS, VIDEO_GIF, QUOTE, DATA_RANDOM,
                                                           WIDTH, HEIGHT, CAPTION, STICKER_PACK_ID,
                                                           STICKER_PACK_KEY, STICKER_ID, STICKER_EMOJI, DATA_HASH, VISUAL_HASH,
                                                           TRANSFORM_PROPERTIES, TRANSFER_FILE, DISPLAY_ORDER,
                                                           UPLOAD_TIMESTAMP };

  public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ROW_ID                 + " INTEGER PRIMARY KEY, " +
                                                                                  MMS_ID                 + " INTEGER, " +
                                                                                  "seq"                  + " INTEGER DEFAULT 0, " +
                                                                                  CONTENT_TYPE           + " TEXT, " +
                                                                                  NAME                   + " TEXT, " +
                                                                                  "chset"                + " INTEGER, " +
                                                                                  CONTENT_DISPOSITION    + " TEXT, " +
                                                                                  "fn"                   + " TEXT, " +
                                                                                  "cid"                  + " TEXT, "  +
                                                                                  CONTENT_LOCATION       + " TEXT, " +
                                                                                  "ctt_s"                + " INTEGER, " +
                                                                                  "ctt_t"                + " TEXT, " +
                                                                                  "encrypted"            + " INTEGER, " +
                                                                                  TRANSFER_STATE         + " INTEGER, " +
                                                                                  DATA                   + " TEXT, " +
                                                                                  SIZE                   + " INTEGER, " +
                                                                                  FILE_NAME              + " TEXT, " +
                                                                                  UNIQUE_ID              + " INTEGER NOT NULL, " +
                                                                                  DIGEST                 + " BLOB, " +
                                                                                  FAST_PREFLIGHT_ID      + " TEXT, " +
                                                                                  VOICE_NOTE             + " INTEGER DEFAULT 0, " +
                                                                                  BORDERLESS             + " INTEGER DEFAULT 0, " +
                                                                                  VIDEO_GIF              + " INTEGER DEFAULT 0, " +
                                                                                  DATA_RANDOM            + " BLOB, " +
                                                                                  QUOTE                  + " INTEGER DEFAULT 0, " +
                                                                                  WIDTH                  + " INTEGER DEFAULT 0, " +
                                                                                  HEIGHT                 + " INTEGER DEFAULT 0, " +
                                                                                  CAPTION                + " TEXT DEFAULT NULL, " +
                                                                                  STICKER_PACK_ID        + " TEXT DEFAULT NULL, " +
                                                                                  STICKER_PACK_KEY       + " DEFAULT NULL, " +
                                                                                  STICKER_ID             + " INTEGER DEFAULT -1, " +
                                                                                  STICKER_EMOJI          + " STRING DEFAULT NULL, " +
                                                                                  DATA_HASH              + " TEXT DEFAULT NULL, " +
                                                                                  VISUAL_HASH            + " TEXT DEFAULT NULL, " +
                                                                                  TRANSFORM_PROPERTIES   + " TEXT DEFAULT NULL, " +
                                                                                  TRANSFER_FILE          + " TEXT DEFAULT NULL, " +
                                                                                  DISPLAY_ORDER          + " INTEGER DEFAULT 0, " +
                                                                                  UPLOAD_TIMESTAMP       + " INTEGER DEFAULT 0, " +
                                                                                  CDN_NUMBER             + " INTEGER DEFAULT 0);";

  public static final String[] CREATE_INDEXS = {
    "CREATE INDEX IF NOT EXISTS part_mms_id_index ON " + TABLE_NAME + " (" + MMS_ID + ");",
    "CREATE INDEX IF NOT EXISTS pending_push_index ON " + TABLE_NAME + " (" + TRANSFER_STATE + ");",
    "CREATE INDEX IF NOT EXISTS part_sticker_pack_id_index ON " + TABLE_NAME + " (" + STICKER_PACK_ID + ");",
    "CREATE INDEX IF NOT EXISTS part_data_hash_index ON " + TABLE_NAME + " (" + DATA_HASH + ");",
    "CREATE INDEX IF NOT EXISTS part_data_index ON " + TABLE_NAME + " (" + DATA + ");"
  };

 
  private @NonNull DataInfo setAttachmentData(@NonNull File destination,
                                              @NonNull InputStream in,
                                              @Nullable AttachmentId attachmentId)
      throws MmsException
  {
    try {
      File                       tempFile          = newFile();
      MessageDigest              messageDigest     = MessageDigest.getInstance("SHA-256");
      DigestInputStream          digestInputStream = new DigestInputStream(in, messageDigest);
      Pair<byte[], OutputStream> out               = ModernEncryptingPartOutputStream.createFor(attachmentSecret, tempFile, false);
      long                       length            = StreamUtil.copy(digestInputStream, out.second);
      String                     hash              = Base64.encodeBytes(digestInputStream.getMessageDigest().digest());

      if (!tempFile.renameTo(destination)) {
        Log.w(TAG, "Couldn't rename " + tempFile.getPath() + " to " + destination.getPath());
        throw new IllegalStateException("Couldn't rename " + tempFile.getPath() + " to " + destination.getPath());
      }

      SQLiteDatabase     database       = databaseHelper.getSignalWritableDatabase();
      Optional<DataInfo> sharedDataInfo = findDuplicateDataFileInfo(database, hash, attachmentId);
      if (sharedDataInfo.isPresent()) {
        Log.i(TAG, "[setAttachmentData] Duplicate data file found! " + sharedDataInfo.get().file.getAbsolutePath());
        if (!destination.equals(sharedDataInfo.get().file) && destination.delete()) {
          Log.i(TAG, "[setAttachmentData] Deleted original file. " + destination);
        }
        return sharedDataInfo.get();
      } else {
        Log.i(TAG, "[setAttachmentData] No matching attachment data found. " + destination.getAbsolutePath());
      }

      return new DataInfo(destination, length, out.first, hash);
    } catch (NoSuchAlgorithmException e) {
      throw new MmsException(e);
    } catch (IOException e) {
      throw new MmsException(e);
    }
  }

}