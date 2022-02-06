package de.hirola.kintojava.model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * All fields with this annotation are persistent storable with kinto.
 * The field must have getter and setter with template get<I><B>A</B>ttribute</I> and set<I><B>A</B>ttribute</I>.
 * <P></P>
 * Supported data types are: <b>String</b>, <b>boolean</b>, <b>integer</b>,<b>long</b>, <b>float</b>, <b>double</b>,
 * <b>java.time.Instant</b> for date and <b>ArrayList</b> of kinto objects
 * <P></P>
 * Internal hint: @Retention(RetentionPolicy.RUNTIME) -> need to mark the annotation as being available at runtime</P>
 *
 * @author Michael Schmidt (Hirola)
 * @since 0.1.0
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Persisted {
}
