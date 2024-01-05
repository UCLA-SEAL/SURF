package org.thoughtcrime.securesms.keyvalue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.signal.core.util.StringStringSerializer;
import org.thoughtcrime.securesms.util.JsonUtils;
import org.whispersystems.signalservice.api.KbsPinData;
import org.whispersystems.signalservice.api.kbs.MasterKey;
import org.whispersystems.signalservice.api.kbs.PinHashUtil;
import org.whispersystems.signalservice.internal.contacts.entities.TokenResponse;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class KbsValues extends SignalStoreValues {

  public synchronized @NonNull MasterKey getOrCreateMasterKey() {
    byte[] blob = getStore().getBlob(MASTER_KEY, null);

    if (blob == null) {
      getStore().beginWrite()
                .putBlob(MASTER_KEY, MasterKey.createNew(new SecureRandom()).serialize())
                .commit();
      blob = getBlob(MASTER_KEY, null);
    }

    return new MasterKey(blob);
  }

}