# DuckZip

Library to handle zip files with a RxJava API

## Installation

Add this to your module's build.gradle:

```groovy
repositories {
        jcenter()
}

dependencies {
    compile 'com.duckma.duckzip:duckzip:1.0.0'
}
```

## Usage

```java
public class MyClass {

    private final DuckZip duckZip = new DuckZip();
    
    duckZip.unzip("/path/to/source/file.zip", "/path/to/output/dir").
            subscribe(new DisposableSubscriber<Float>() {
                @Override
                public void onNext(Float aFloat) {
                    System.out.println("Unzip progress: " + Float.toString(aFloat));
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("Error: " + t.getLocalizedMessage());
                }

                @Override
                public void onComplete() {
                    System.out.println("Unzip complete");
                }
            });

}
```

## License

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
