# Changelog

* For newer versions, see [GitHub Releases](https://github.com/jenkinsci/structs-plugin/releases)

## Version 1.22 (Feb 17, 2021)

-   Internal: Use plugin BOM and adapt to change in GitSCM behavior to fix PCT failures ([PR-78](https://github.com/jenkinsci/structs-plugin/pull/78))

## Version 1.21 (Feb 04, 2021)

-   Move documentation from Wiki to Github ([PR-53](https://github.com/jenkinsci/structs-plugin/pull/53))
-   Internal - Enable dependabot to automatically get dependency updates ([PR-56](https://github.com/jenkinsci/structs-plugin/pull/56))

## Version 1.20 (Jul 29, 2019)

-   [JENKINS-33217](https://issues.jenkins-ci.org/browse/JENKINS-33217) Log a warning when additional parameters are passed into `DescribableModel` objects, such as Pipeline steps, since these parameters are currently being ignored.
-   Internal - Update parent pom ([PR-46](https://github.com/jenkinsci/structs-plugin/pull/46)) and fix unit tests ([PR-50](https://github.com/jenkinsci/structs-plugin/pull/50))

## Version 1.19 (Apr 25, 2019)

-   [JENKINS-57218](https://issues.jenkins-ci.org/browse/JENKINS-57218) -
    Prevent NullPointerException from SymbolLookup after Job DSL update without Configuration as Code

## Version 1.18 (Apr 25, 2019)

-    [JENKINS-44892](https://issues.jenkins-ci.org/browse/JENKINS-44892) Add new `CustomDescribableModel` API to allow custom instantiation and uninstantiation for `DescribableModel` for advanced use cases.

## Version 1.17 (Oct 05, 2018)

-    [JENKINS-53917](https://issues.jenkins-ci.org/browse/JENKINS-53917) Reverting change in 1.16.

## Version 1.16 (Oct 04, 2018)

-   Analysis problems with `ChoiceParameterDefinition`.

## Version 1.15 (Sept 25, 2018)

-   Automatically coerce `String` to a number or boolean when a parameter expects a number or boolean.

## Version 1.14 (Feb 14, 2018)

-   Parameter handling (dependency for [JENKINS-37215](https://issues.jenkins-ci.org/browse/JENKINS-37215))

## Version 1.13 (Feb 1, 2018)

-   Hotfix for **sigh** Groovy-related madness, partially reverting memory optimizations from 1.12
-   Minor correction to `DescribableModel` caching lookup
    -   Eliminates any risk looking up `DescribableModel`s if different plugins somehow define identical but incompatible Describable classes in the same package and class

## Version 1.12 (Feb 1, 2018)

-   **Major Optimizations**:
    -   Cache negative-hits in `Symbol` lookup (i.e. "no match"), eliminating needless classloading and iteration over classes. 
    -   Cache DescribableModels, eliminating classloading associated with creation.  
    -   Net result: **huge** reduction in disk reads, lock contention (classloading), CPU use, and memory garbage generated.
-   Minor optimization: reduce memory use and garbage generation (collection pre-sizing and use of Singleton collections)
-   [JENKINS-46122](https://issues.jenkins-ci.org/browse/JENKINS-46122) Report base class name when symbol couldn't be resolved

## Version 1.10 (Aug 03, 2017)

-   Javadoc improvements.
-   Adjusting `annotation-indexer` version to match current core baseline, avoiding POM warnings in plugins depending on this one.

## Version 1.9 (Jun 26, 2017)

-   [JENKINS-45130](https://issues.jenkins-ci.org/browse/45130) When uninstantiating, qualify otherwise ambiguous class names for array and list properties.

## Version 1.8 (Jun 15, 2107)

-   [JENKINS-44864](https://issues.jenkins-ci.org/browse/44864) When uninstantiating, suppress values from `@Deprecated` setters where the values have no effect on the resulting object. 

## Version 1.7 (May 25, 2017)

-   [JENKINS-43337](https://issues.jenkins-ci.org/browse/JENKINS-43337) Snippet generation should qualify otherwise ambiguous class names.

-   [JENKINS-34464](https://issues.jenkins-ci.org/browse/JENKINS-34464) Allow binding of parameters of type `Result`, for example in the `upstream` trigger.

-   [JENKINS-31967](https://issues.jenkins-ci.org/browse/JENKINS-31967) Handle remaining primitive types for parameters, for example `double` in `junit` configuration.

## Version 1.6 (Feb 13, 2017)

-   [JENKINS-38157](https://issues.jenkins-ci.org/browse/JENKINS-38157) Better diagnostics.
-   Allow Groovy `GString` to be used in more places.
-   API to check deprecation status of a type.

## Version 1.5 (Aug 30, 2016)

-   [JENKINS-37820](https://issues.jenkins-ci.org/browse/JENKINS-37820) Stack overflow in 1.4 under certain conditions.

## Version 1.4 (Aug 26, 2016)

-   [JENKINS-37403](https://issues.jenkins-ci.org/browse/JENKINS-37403) API for getting `@Symbol` off an `Object`.

## Version 1.3 (Jul 28, 2016)

-   [JENKINS-29922](https://issues.jenkins-ci.org/browse/JENKINS-29922) Support for `@Symbol` in `DescribableModel`.

## Version 1.2.0 (Jun 17, 2016)

-  ℹ️ Added method to query deprecated methods ([PR #5](https://github.com/jenkinsci/structs-plugin/pull/5))
-  ❌ Improve diagnostics for mismatched types ([JENKINS-34070](https://issues.jenkins-ci.org/browse/JENKINS-34070))
-  ❌ Prevent recursions in the DescribableModel.toString() method ([PR #3](https://github.com/jenkinsci/structs-plugin/pull/3), related to [JENKINS-32925](https://issues.jenkins-ci.org/browse/JENKINS-32925))

## Version 1.1.1 (Jun 16, 2016)

-  ❌ Fix URL to the plugin's Wiki page in order to get it listed in Jenkins Update Center again ([JENKINS-35918](https://issues.jenkins-ci.org/browse/JENKINS-35918))

## Version 1.1 (Mar 22, 2016)

-   ℹ️ Many small commits made by [kohsuke](https://github.com/kohsuke)

## Version 1.0 (Mar 18, 2016)

-   initial version
