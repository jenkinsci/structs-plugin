package org.jenkinsci.plugins.structs.describable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.Secret;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LogRecorder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

@WithJenkins
class Security3371Test {

    private final LogRecorder warningLogger = new LogRecorder().record(DescribableModel.class, Level.WARNING).capture(10);

    @Test
    void secretsAreNotLoggedWhenInstantiationFails(JenkinsRule r) {
        String password = "password";

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new DescribableModel<>(Cred.class).instantiate(Map.of("username", "username", "pwd", Secret.fromString(password)), null));

        assertThat(e.getMessage(), containsString("Secrets are involved, so details are available on more verbose logging levels."));
        assertThat(e.getMessage(), not(containsString("pwd=" + password)));
    }

    @Test
    void secretsAreNotLoggedInUninstantiatedDescribable(JenkinsRule r) {
        String password = "password";
        ServerModel serverModel = new ServerModel("http://localhost", new Cred("username", password));
        new DescribableModel<>(ServerModel.class).uninstantiate2(serverModel);
        assertThat(warningLogger.toString(), not(containsString("pwd=" + password)));
    }

    static final class Cred extends AbstractDescribableImpl<Cred> implements Serializable {
        String username;
        Secret pwd;
        String deprecated;

        @DataBoundConstructor
        public Cred(String username, String pwd) {
            this.username = username;
            this.pwd = Secret.fromString(pwd);
        }

        public String getUsername() {
            return username;
        }

        public Secret getPwd() {
            return pwd;
        }

        public String getDeprecated() {
            return deprecated;
        }

        @DataBoundSetter
        @Deprecated
        public void setDeprecated(String deprecated) {
            this.deprecated = deprecated;
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<Cred> {
        }
    }

    static final class ServerModel extends AbstractDescribableImpl<ServerModel> implements Serializable {
        private String host;
        private Cred cred;

        @DataBoundConstructor
        public ServerModel(String host, Cred cred) {
            this.host = host;
            this.cred = cred;
        }

        public String getHost() {
            return host;
        }

        public Cred getCred() {
            return cred;
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<ServerModel> {
        }
    }
}
