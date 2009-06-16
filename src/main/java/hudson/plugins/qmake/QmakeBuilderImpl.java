package hudson.plugins.qmake;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.kohsuke.stapler.framework.io.IOException2;

public class QmakeBuilderImpl {

  private static final String CONFIG_PREFIX = "CONFIG+=";

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

  String buildQMakeCall(String qmakeBin, String projectFile, String extraConfig ) {
    String qmakeCall = qmakeBin + " "
                     + "\"" + projectFile + "\"";
		     
    if (!extraConfig.isEmpty()) {
      qmakeCall += " \"" + CONFIG_PREFIX + extraConfig + "\"";
    }
    return qmakeCall;
  }
}
