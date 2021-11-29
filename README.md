# Fake SFTP Server Extension

![build](https://github.com/ppi-ag/fake-sftp-server-extension/actions/workflows/publish.yaml/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ppi-ag_fake-sftp-server-extension&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ppi-ag_fake-sftp-server-extension)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ppi-ag_fake-sftp-server-extension&metric=coverage)](https://sonarcloud.io/summary/new_code?id=ppi-ag_fake-sftp-server-extension)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=ppi-ag_fake-sftp-server-extension&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=ppi-ag_fake-sftp-server-extension)

Fake SFTP Server Extension is a JUnit extension that runs an in-memory SFTP server while your tests are running. It uses
the SFTP server of the
[Apache SSHD](http://mina.apache.org/sshd-project/index.html) project.

Fake SFTP Server Extension is published under the
[MIT license](http://opensource.org/licenses/MIT). It requires at least Java 11. Please
[open an issue](https://github.com/ppi-ag/fake-sftp-server-extension/issues/new)
if you want to use it with an older version of Java.

This repository is based on the [Fake SFTP Server Rule](https://github.com/stefanbirkner/fake-sftp-server-rule)
by [@stefanbirkner](https://github.com/stefanbirkner).

There is an alternative to Fake SFTP Server Extension that is independent of the test framework. Its name is
[Fake SFTP Server Lambda](https://github.com/stefanbirkner/fake-sftp-server-lambda).

## Installation

Fake SFTP Server Extension is available from
[Maven Central](https://search.maven.org/#search|ga|1|fake-sftp-server-extension).

```xml
<dependency>
  <groupId>de.ppi</groupId>
  <artifactId>fake-sftp-server-extension</artifactId>
  <version>1.0.2</version>
  <scope>test</scope>
</dependency>
```
## Usage

The Fake SFTP Server Extension is used by adding it to your test class.

```java
import FakeSftpServerExtension;

public class TestClass {

  @RegisterExtension
  public final FakeSftpServerExtension sftpServer = new FakeSftpServerExtension();

  // ...
}
```

This extension starts a server before your test and stops it afterwards.

By default, the SFTP server listens on an auto-allocated port. During the test this port can be obtained
by `sftpServer.getPort()`. It can be changed by calling `setManualPort(int)`. If you do this from within a test then the
server gets restarted. The time-consuming restart can be avoided by setting the port immediately after creating the
extension.

```java
public class TestClass {

  @RegisterExtension
  public final FakeSftpServerExtension sftpServer = new FakeSftpServerExtension()
      .setManualPort(1234);

  // ...
}
```

You can interact with the SFTP server by using the SFTP protocol with password
authentication. By default, the server accepts every pair of username and
password, but you can restrict it to specific pairs.

```java
public class TestClass {

  @RegisterExtension
  public final FakeSftpServerExtension sftpServer = new FakeSftpServerExtension()
      .addUser("username", "password");

  // ...
}
```

It is also possible to do this during the test using the same method.

### Testing code that reads files

If you test code that reads files from an SFTP server then you need a server that provides these files. Fake SFTP Server
Extension provides a shortcut for uploading files to the server.

```java
@Test
public void testTextFile() {
  sftpServer.putFile("/directory/file.txt", "content of file", UTF_8);

  // now you can download the file, just connect via SFTP
}

@Test
public void testBinaryFile() {
  byte[] content = createContent();
  sftpServer.putFile("/directory/file.bin", content);

  // now you can download the file, just connect via SFTP
}
```

Test data that is provided as an input stream can be uploaded directly from that
input stream. This is very handy if your test data is available as a resource.

```java
@Test
public void testFileFromInputStream() {
  InputStream is = getClass().getResourceAsStream("data.bin");
  sftpServer.putFile("/directory/file.bin", is);

  // now you can download the file, just connect via SFTP
}
```

If you need an empty directory then you can use the method
`createDirectory(String)`.

```java
@Test
public void testDirectory() {
  sftpServer.createDirectory("/a/directory");

  // code that reads from or writes to that directory
}
```

You may create multiple directories at once with `createDirectories(String...)`.

```java
@Test
public void testDirectories() {
  sftpServer.createDirectories(
    "/a/directory",
    "/another/directory"
  );

  // code that reads from or writes to that directories
}
```


### Testing code that writes files

If you test code that writes files to an SFTP server then you need to verify the upload. Fake SFTP Server Extension
provides a shortcut for getting the file's content from the server.

```java
@Test
public void testTextFile() {
  // code that uploads the file

  String fileContent = sftpServer.getFileContent("/directory/file.txt", UTF_8);
  ...
}

@Test
public void testBinaryFile() {
  // code that uploads the file

  byte[] fileContent = sftpServer.getFileContent("/directory/file.bin");
  ...
}
```

### Testing existence of files

If you want to check whether a file hast been created or deleted then you can
verify that it exists or not.

```java
@Test
public void testFile() {
  // code that uploads or deletes the file

  boolean exists = sftpServer.existsFile("/directory/file.txt");
  ...
}
```

The method returns `true` iff the file exists, and it is not a directory.

### Delete all files

If you want to reuse the SFTP server then you can delete all files and directories on the SFTP server. (This is rarely
necessary because the extension itself takes care that every test starts and ends with a clean SFTP server.)

    sftpServer.deleteAllFilesAndDirectories()

## Contributing

You have three options if you have a feature request, found a bug or simply have a question about Fake SFTP Server
Extension.

* [Write an issue.](https://github.com/ppi-ag/fake-sftp-server-extension/issues/new)
* Create a pull request. (See [Understanding the GitHub Flow](https://guides.github.com/introduction/flow/index.html))


## Development Guide

Fake SFTP Server Extension is build with [Maven](http://maven.apache.org/). If you want to contribute code then

* Please write a test for your change.
* Ensure that you didn't break the build by running `mvn verify -Dgpg.skip`.
* Fork the repo and create a pull request. (See [Understanding the GitHub Flow](https://guides.github.com/introduction/flow/index.html))

The basic coding style is described in the
[EditorConfig](http://editorconfig.org/) file `.editorconfig`.


## Release Guide

* Select a new version according to the
  [Semantic Versioning 2.0.0 Standard](http://semver.org/).
* Set the new version in `pom.xml` and in the `Installation` section of
  this readme.
* Commit the modified `pom.xml` and `README.md`.
* Run `mvn clean deploy` with JDK 8.
* Add a tag for the release: `git tag fake-sftp-server-extension-X.X.X`
