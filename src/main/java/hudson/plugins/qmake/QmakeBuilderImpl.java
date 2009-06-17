package hudson.plugins.qmake;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class QmakeBuilderImpl {

  private static final String CONFIG_PREFIX = "CONFIG+=";
  private static final String QMAKE_DEFAULT = "qmake";

  String qmakeBin;

  public enum PreparePathOptions
  {
    CHECK_FILE_EXISTS() {
      @Override
	public void process(File file) throws IOException {
	  if (!file.exists() || !file.canRead()) {
	    throw new FileNotFoundException(file.getAbsolutePath());
	  }
	}
    };

    public abstract void process(File file) throws IOException;
  };
	
  public QmakeBuilderImpl() {
    super();
  }
	
  String preparePath(Map<String, String> envVars, String path, PreparePathOptions ppOption) throws IOException {
    path = path.trim();
    Set<String> keys = envVars.keySet();
    for (String key : keys) {
      path = path.replaceAll("\\$" + key, envVars.get(key));
    }

    File file = new File(path);
    if (!file.isAbsolute()) {
      path = envVars.get("WORKSPACE") + "/" + path;
    }
    file = new File(path);
    ppOption.process(file);
    return file.getPath();
  }

  void setQmakeBin(Map<String, String> envVars,
                   String globalQmakeBin, boolean isWindows ) {
    qmakeBin = QMAKE_DEFAULT;

    if (globalQmakeBin != null && globalQmakeBin.length() > 0) {
      File fileInfo = new File( globalQmakeBin );
      if (fileInfo.exists()) qmakeBin = globalQmakeBin;
    }

    if (envVars.containsKey( "QTDIR" ) ) {
      String checkName = envVars.get("QTDIR") + "/bin/qmake";
      if (isWindows) {
	checkName += ".exe";
      }
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
