Example usage
=======================

In order to run the static analysis run the following command:
```
java -jar ./mir-sa.jar ./path-to-npm-package-folder
```

You can set the following additional flags:
* npm.pkg.level - by default the static analysis runs at file level, if this flag is set the analyses aggregates the permissions for the target npm module
* maybe.reaching - the static analysis uses a must-be-reaching def-use analysis. Set this flag if you want to use a maybe-reaching def instead
* target.module - set this flag if you want the static analysis to only produce permissions for a given module that the analyzed module uses

An example command with all these flags set is:
```
java -Dmaybe.reaching=true -Dnpm.pkg.level=true -Dtarget.module=argparse -jar mir-sa.jar ./path-to-npm-package-folder
```

Partial Results
=======================
* Diff between may-def-use and must-def-use on 192 packages: 2250 (may) vs. 2217 (must) entries. 
* For privilege reduction results check the pdf in the figs folder.
