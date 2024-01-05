/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio;

import alluxio.annotation.PublicApi;
import alluxio.exception.InvalidPathException;
import alluxio.uri.Authority;
import alluxio.uri.NoAuthority;
import alluxio.uri.URI;
import alluxio.util.URIUtils;
import alluxio.util.io.PathUtils;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This class represents a URI in the Alluxio system. This {@link AlluxioURI} can represent
 * resources in the Alluxio namespace, as well as UFS namespaces.
 *
 * {@link AlluxioURI} supports more than just strict {@link URI}. Some examples:
 *   * Windows paths
 *     * C:\
 *     * D:\path\to\file
 *     * E:\path\to\skip\..\file
 *   * URI with multiple scheme components
 *     * scheme://host:123/path
 *     * scheme:part2//host:123/path
 *     * scheme:part2://host:123/path
 *     * scheme:part2:part3//host:123/path
 *     * scheme:part2:part3://host:123/path
 *
 * Does not support fragment in the URI.
 */
@PublicApi
@ThreadSafe
public final class AlluxioURI implements Comparable<AlluxioURI>, Serializable {
  private static final long serialVersionUID = -1207227692436086387L;

  public static final String SEPARATOR = "/";
  public static final String CUR_DIR = ".";
  public static final String WILDCARD = "*";

  public static final AlluxioURI EMPTY_URI = new AlluxioURI("");

  /** A {@link URI} is used to hold the URI components. */
  private final URI mUri;

  // Cached string version of the AlluxioURI
  private String mUriString = null;

 

  /**
   * Computes the hash of this URI, with SHA-256, or MD5, or simple hashCode().
   *
   * @param uri the alluxio path uri
   *
   * @return HEX encoded hash string
   */
  public static String hash(String uri) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(uri.getBytes());
      return Hex.encodeHexString(md.digest()).toLowerCase();
    } catch (NoSuchAlgorithmException e) {
      /* No actions. Continue with other hash method. */
    }

    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(uri.getBytes());
      return Hex.encodeHexString(md.digest()).toLowerCase();
    } catch (NoSuchAlgorithmException e) {
      /* No actions. Continue with other hash method. */
    }

    // Cannot find SHA-256 or MD5. Fall back to use simple hashCode, which is probable to conflict.
    return Hex.encodeHexString(String.valueOf(uri.hashCode()).getBytes()).toLowerCase();
  }
}