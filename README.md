## SimpleItemAnimator

This library is provided simple animator on the Recyclerview.

### add dependencies

```
repositories {
    maven { url 'http://daneko.github.io/m2repo/repository' }
}

dependencies {
    compile 'com.github.daneko:simple-item-animator:0.0.1-SNAPSHOT'
}
```

### sample code

```java
// set animation only!
recyclerView.setItemAnimator(
        SimpleItemAnimator.builder().
                preAddAnimationState(v -> {
                    ViewCompat.setAlpha(v, 1);
                    ViewCompat.setScaleX(v, 0);
                    ViewCompat.setScaleY(v, 0);
                    return Unit.unit();
                }).
                addAnimation(animator -> animator.scaleX(1).scaleY(1)).
                addDuration(500).
                removeAnimation(animator ->
                        animator.translationXBy(recyclerView.getWidth())).
                preChangeAnimationState((oldV, newVOpt, param) -> {
                    ViewCompat.setAlpha(oldV, 1);
                    newVOpt.foreach(newV -> {
                        ViewCompat.setAlpha(newV, 0);
                        ViewCompat.setRotationX(newV, -180);
                        return Unit.unit();
                    });
                    return Unit.unit();
                }).
                changeAnimation(
                        (forOld, param) -> forOld.rotationX(180).alpha(0),
                        (forNew, param) -> forNew.rotationX(0).alpha(1)).
                isChangeAnimationMix(false).
                changeDuration(500).
                build());
```

### use library

* [Lombok](http://projectlombok.org/) [(license MIT)](https://github.com/rzwitserloot/lombok/blob/master/LICENSE)
* [RxAndroid](https://github.com/ReactiveX/RxAndroid) ([license apache license 2.0](https://github.com/ReactiveX/RxAndroid/blob/0.x/LICENSE))
* [retrolambda](https://github.com/orfjackal/retrolambda) ([license apache license 2.0](https://github.com/orfjackal/retrolambda/blob/master/LICENSE.txt))
* [FunctionalJava](http://www.functionaljava.org/) ([license BSD 3](https://github.com/functionaljava/functionaljava#license))
* [JSR305](https://code.google.com/p/jsr-305/) ([license BSD 3](http://opensource.org/licenses/BSD-3-Clause))
* [slf4j](http://slf4j.org/) ([license MIT](http://slf4j.org/license.html))

and `com.android.support:recyclerview-v7:21.0.0`



### use library in sample app

* [ButterKnife](http://jakewharton.github.io/butterknife/) ([license apache license 2.0](https://github.com/JakeWharton/butterknife/blob/master/LICENSE.txt))
* [logback android](http://tony19.github.io/logback-android/) (license EPL)

### local.properties sample

```
sdk.dir=/usr/local/opt/android-sdk
java8_home=/Library/Java/JavaVirtualMachines/jdk1.8.0_20.jdk/Contents/Home
java7_home=/Library/Java/JavaVirtualMachines/jdk1.7.0_67.jdk/Contents/Home
```

### known issue

when app build. occur compile error, like

```
SimpleItemAnimator/library/src/main/java/com/github/daneko/simpleitemanimator/AbstAnimationExecutor.java:4: error: cannot find symbol
import android.support.v4.view.ViewPropertyAnimatorListener;
                              ^
  symbol:   class ViewPropertyAnimatorListener
  location: package android.support.v4.view
1 error

 FAILED
```
retry compile, and compile success.

### License

(The MIT License)

Copyright (c) 2014 daneko

