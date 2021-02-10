package com.netflix.spinnaker.security;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Marker interface indicating the object contains additional fields describing the user such as
 * name, contact info, etc.
 */
public interface UserAttributes {

  /**
   * Allows an implementing class to filter out any sensitive attributes and return an object
   * suitable for rendering.
   *
   * @return a jackson serializable object that only includes attributes that should be publicly
   *     exposed back to the user through the API
   */
  @JsonIgnore
  default Object getPublicAttributes() {
    return this;
  }
}
