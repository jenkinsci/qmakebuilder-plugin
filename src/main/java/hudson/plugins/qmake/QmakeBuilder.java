package hudson.plugins.qmake;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes <tt>qmake</tt> and <tt>make</tt> as the build process for a QT-based build.
 *
 * @author Tyler Mace
 */
public class QmakeBuilder extends Builder {

    private static final String QMAKE = "qmake";

    private String projectFile;
    private String extraConfig;
    private boolean cleanBuild;

    private QmakeBuilderImpl builderImpl;
    
    @DataBoundConstructor
    public QmakeBuilder(String projectFile, String extraConfig) {
      this.projectFile = projectFile;
      this.extraConfig = extraConfig;
      this.cleanBuild = false;
      builderImpl = new QmakeBuilderImpl();		
    }

    public String getProjectFile() {
      return this.projectFile;
    }
    
    public String getExtraConfig() {
      return this.extraConfig;
    }

    public boolean getCleanBuild() {
      return this.cleanBuild;
    }
    
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
      listener.getLogger().println("MODULE: " + build.getProject().getModuleRoot());

      if (builderImpl == null) {
	builderImpl = new QmakeBuilderImpl();
      }
      EnvVars envs = build.getEnvironment(listener);

      String theProjectFile;
      String theInstallDir;
      try {

	//builderImpl.preparePath(envs, this.buildDir, 
	//    QmakeBuilderImpl.PreparePathOptions.CREATE_NEW_IF_EXISTS);
	theProjectFile = builderImpl.preparePath(envs, this.projectFile,
	    QmakeBuilderImpl.PreparePathOptions.CHECK_FILE_EXISTS);
	//theInstallDir = builderImpl.preparePath(envs, this.installDir,
	//    QmakeBuilderImpl.PreparePathOptions.CREATE_NEW_IF_EXISTS);
      } catch (IOException ioe) {
	listener.getLogger().println(ioe.getMessage());
	return false;
      }
      //    	catch (InterruptedException e) {
      //    		listener.getLogger().println(e.getMessage());
      //			return false;
      //		}

      String qmakeBin = QMAKE;

      String qmakePath = getDescriptor().qmakePath();
      if (qmakePath != null && qmakePath.length() > 0) {
	qmakeBin = qmakePath;
      }
      String qmakeCall = builderImpl.buildQMakeCall(qmakeBin, theProjectFile, extraConfig );

      File fileInfo = new File(theProjectFile);
      FilePath workDir = new FilePath(build.getProject().getWorkspace(),
                                      fileInfo.getParent()); 
      listener.getLogger().println("QMake call : " + qmakeCall);
      listener.getLogger().println("QMake bin : " + qmakeBin);
      listener.getLogger().println("QMake project file : " + theProjectFile);

      try {
	Proc proc = launcher.launch(qmakeCall, envs, listener.getLogger(), workDir);
	int result = proc.join();
	if (result != 0) return false;

	String makeExe = "make";
	if (!launcher.isUnix()) makeExe = "nmake";

	proc = launcher.launch(makeExe, envs, listener.getLogger(), workDir);
	result = proc.join();

	if (result != 0) {
	  return false;
	} else {
	  return true;
	}
      } catch (IOException e) {
	e.printStackTrace();
      } catch (InterruptedException ie) {
	ie.printStackTrace();
      }
      return false;
    }

    public DescriptorImpl getDescriptor() {
      return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link QmakeBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>views/hudson/plugins/hello_world/QmakeBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> {
      /**
       * To persist global configuration information,
       * simply store it in a field and call save().
       *
       * <p>
       * If you don't want fields to be persisted, use <tt>transient</tt>.
       */
      private String qmakePath;

      public DescriptorImpl() {
	super(QmakeBuilder.class);
	load();
      }

      /**
       * Performs on-the-fly validation of the form field 'projectFile'.
       *
       * @param value
       */
      public FormValidation doCheckProjectFile(@QueryParameter final String value) throws IOException, ServletException {
	if(value.length()==0)
	  return FormValidation.error("Please set a project file");
	if(value.length() < 1)
	  return FormValidation.warning("Isn't the name too short?");

	File file = new File(value);
	if (file.isDirectory())
	  return FormValidation.error("Project file is a directory");

	//TODO add more checks
	return FormValidation.ok();
      }

      /**
       * This human readable name is used in the configuration screen.
       */
      public String getDisplayName() {
	return "QMake Build";
      }

      public boolean configure(StaplerRequest req, JSONObject o) throws FormException {
	// to persist global configuration information,
	// set that to properties and call save().
	qmakePath = o.getString("qmakePath");
	save();
	return super.configure(req, o);
      }

      public String qmakePath() {
	return qmakePath;
      }
    }
}

