-assumenosideeffects class org.slf4j.Logger {
      public *** trace(...);
      public *** debug(...);
}

-assumenosideeffects class org.slf4j.Logger {
      public *** trace(...);
      public *** debug(...);
}

-assumenosideeffects interface org.slf4j.Logger {
      public *** trace(...);
      public *** debug(...);
}

-assumenosideeffects class * implements org.slf4j.Logger {
      public *** trace(...);
      public *** debug(...);
}

-assumenosideeffects class * implements org.slf4j.Logger {
      public void trace(java.lang.String);
      public void trace(java.lang.String, java.lang.Object);
      public void trace(java.lang.String, java.lang.Object, java.lang.Object);
      public void trace(java.lang.String, java.lang.Object[]);
      public void trace(java.lang.String, java.lang.Throwable);
      public void trace(org.slf4j.Marker, java.lang.String);
      public void trace(org.slf4j.Marker, java.lang.String, java.lang.Object);
      public void trace(org.slf4j.Marker, java.lang.String, java.lang.Object, java.lang.Object);
      public void trace(org.slf4j.Marker, java.lang.String, java.lang.Object[]);
      public void trace(org.slf4j.Marker, java.lang.String, java.lang.Throwable);
      public void debug(java.lang.String);
      public void debug(java.lang.String, java.lang.Object);
      public void debug(java.lang.String, java.lang.Object, java.lang.Object);
      public void debug(java.lang.String, java.lang.Object[]);
      public void debug(java.lang.String, java.lang.Throwable);
      public void debug(org.slf4j.Marker, java.lang.String);
      public void debug(org.slf4j.Marker, java.lang.String, java.lang.Object);
      public void debug(org.slf4j.Marker, java.lang.String, java.lang.Object, java.lang.Object);
      public void debug(org.slf4j.Marker, java.lang.String, java.lang.Object[]);
      public void debug(org.slf4j.Marker, java.lang.String, java.lang.Throwable);
}
