package com.airbnb.lottie.network;


import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

import com.airbnb.lottie.utils.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Helper class to save and restore animations fetched from an URL to the app disk cache.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class NetworkCache {

  @NonNull
  private final LottieNetworkCacheProvider cacheProvider;

  public NetworkCache(@NonNull LottieNetworkCacheProvider cacheProvider) {
    this.cacheProvider = cacheProvider;
  }

  
  private static String getMD5(String input, int maxLength) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      // For some reason, md5 doesn't exist, return a substring.
      // This should never happen.
      return input.substring(0, maxLength);
    }
    byte[] messageDigest = md.digest(input.getBytes());
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < messageDigest.length; i++) {
      byte b = messageDigest[i];
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}