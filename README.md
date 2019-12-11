# Structs plugin
[![Jenkins Plugins](https://img.shields.io/jenkins/plugin/v/structs)](https://plugins.jenkins.io/structs)
[![Changelog](https://img.shields.io/github/v/tag/jenkinsci/structs-plugin?label=changelog)](https://github.com/jenkinsci/structs-plugin/blob/master/CHANGELOG.md)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/structs?color=blue)](https://plugins.jenkins.io/structs)


Library plugin for DSL plugins that need concise names for Jenkins extensions

## Overview

Jenkins has many DSL like plugins that require having short concise names for implementations of the extension points and other Jenkins objects. For example, [Job DSL Plugin](https://plugins.jenkins.io/job-dsl) refers to each `SCM` extension by its short name. The same goes for pipeline plugin.

It benefits users that these DSL plugins use consistent names. This plugin, together with the `@Symbol` annotation, allow plugin developers to name their extension and have all the DSL plugins recognize them.

## Usage for developers creating any plugins

To allow all the DSL plugins to refer to your extensions by the same name, put `@Symbol` annotation along side your `@Extension`. The symbol name must be a valid Java identifier, and it should be short and concise. To keep the symbol name short, it needs to be only unique within the extension point. For example, `GitSCM` and `GitToolInstaller` should both have a symbol name `git`, because they are from different extension points. For compatibility reasons with DSL plugins that predates the structs plugin, some extensions may have legacy names that do not follow this convention.

``` java
public class GitSCM {
   ...

   @Extension @Symbol("git")
   public static class DescriptorImpl extends SCMDescriptor {
      ...
   }
}
```

If you are targeting 1.x version of Jenkins, you must also add the following dependency to your plugin POM:

``` xml
    <dependency>
        <groupId>org.jenkins-ci.plugins</groupId>
        <artifactId>structs</artifactId>
        <version>1.2</version>
    </dependency>
```

## Usage for DSL plugin developers

Look up an extension by its symbol:

``` java
@Extension @Symbol("foo")
public class FooGlobalConfiguration extends GlobalConfiguration {
   ...
}

// this yields the FooGlobalConfiguration instance
SymbolLookup.get().find(GlobalConfiguration.class, "foo")
```

Construct a `Describable` object from a key/value pairs, much like how [Structured Form Submission](https://wiki.jenkins.io/display/JENKINS/Structured+Form+Submission) does it via `@DataBoundConstructor`:

``` java
new DescribableModel(Mailer.class).instantiate(
        Collections.singletonMap("recipients", "kk@kohsuke.org"))
```

## Version history

Please refer to the [Changelog](CHANGELOG.md)
