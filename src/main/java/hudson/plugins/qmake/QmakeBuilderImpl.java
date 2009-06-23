package hudson.plugins.qmake;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.io.PrintStream;
import hudson.FilePath;

public class QmakeBuilderImpl {

  private static final String QMAKE_DEFAULT = "qmake";

  String qmakeBin;
  boolean isWindows;

  public QmakeBuilderImpl() {
    super();
  }
	
  String preparePath(Map<String, String> envVars, String path, boolean isWindows, PrintStream logger) throws IOException {
    this.isWindows = isWindows;
    path = path.trim();

    Set<String> keys = envVars.keySet();
    for (String key : keys) {
      path = path.replaceAll("\\$" + key, envVars.get(key));
    }

    File file = new File(path);
    if (!file.isAbsolute()) {
      String tmp = path;
      path = envVars.get("WORKSPACE");
      if (isWindows) path += "\\";
      else path += "/";
      path += tmp;
    }

    return path;
  }

  void setQmakeBin(Map<String, String> envVars,
                   String globalQmakeBin, boolean isWindows ) {
    qmakeBin = QMAKE_DEFAULT;

    if (globalQmakeBin != null && globalQmakeBin.length() > 0) {
      File fileInfo = new File( globalQmakeBin );
      if (fileInfo.exists()) qmakeBin = globalQmakeBin;
    }

    if (envVars.containsKey( "QTDIR" ) ) {
      String checkName = envVars.get("QTDIR");
      if (isWindows) checkName += "\\bin\\qmake.exe";
      else           checkName += "/bin/qmake";

      File fileInfo = new File( checkName );
      if (fileInfo.exists()) qmakeBin = checkName;
    }
  }

  String buildQMakeCall(String projectFile, String extraArguments ) {
    String qmakeCall = qmakeBin + " -r \"" + projectFile + "\"";
		     
    if (!extraArguments.isEmpty()) {
      qmakeCall += " " + extraArguments;
    }
    return qmakeCall;
  }
}
