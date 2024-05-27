<p align="center">
  <small><i>Learn Java, code Minecraft plugins and launch a unique network from the ground up in 20 days (without experience):</i></small>
  <a href="https://mineacademy.org/project-orion?st=github&sc=bungeefoundation&utm_source=github&utm_medium=overview&utm_campaign=bungeefoundation">
    <img src="https://i.imgur.com/SVHA9Kf.png" />
  </a>
</p>

[![](https://jitpack.io/v/kangarko/BungeeFoundation.svg)](https://jitpack.io/#kangarko/BungeeFoundation)

BungeeFoundation is a library for bootstrapping BungeeCord plugins.

## Using

1. Install BungeeFoundation as a dependency from our [JitPack](https://jitpack.io/#kangarko/bungeefoundation/):
```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```
```xml
<dependency>
  <groupId>com.github.kangarko</groupId>
  <artifactId>BungeeFoundation</artifactId>
  <version>REPLACE_WITH_VERSION</version>
</dependency>
```
2. Relocate BungeeControl when shading. Here is a snippet for Maven to place inside <plugins> section. You need to change the shadedPattern to match your own package name.
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.2.4</version>
  <executions>
    <execution>
      <phase>package</phase>
      <goals>
        <goal>shade</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <createDependencyReducedPom>false</createDependencyReducedPom>
    <artifactSet>
      <includes>
        <include>org.mineacademy:BungeeFoundation*</include>
      </includes>
    </artifactSet>
    <relocations>
      <relocation>
        <pattern>org.mineacademy</pattern>
        <shadedPattern>your.package.name.lib</shadedPattern>
        <excludes>
          <exclude>your.package.name.*</exclude>
        </excludes>
      </relocation>
    </relocations>
  </configuration>
</plugin>
```
3. Make your main class extend SimplePlugin from Foundation. [See this sample implementation](https://bitbucket.org/kangarko/bungeecontrol/src/master/src/main/java/org/mineacademy/bungeecontrol/BungeeControl.java) for how it works.

 
### Important Licencing Information

© MineAcademy.org

If you are a paying student of MineAcademy.org then you are granted full
unlimited licence to use, modify and reproduce Foundation both commercially
and non-commercially, for yourself, your team or network. You can also
modify the library however you like and include it in your plugins you publish
or sell without stating that you are using this library.

If you are not a paying student of MineAcademy.org then you may
use this library for non-commercial purposes only. You are allowed
to make changes to this library however as long as those are only
minor changes you must clearly attribute that you are using Foundation
in your software.

For both parties, do not sell or claim any part of this library as your own.
All infringements will be prosecuted.

No guarantee - this software is provided AS IS, without any guarantee on its
functionality. We made our best efforts to make Foundation an enterprise-level
solution for anyone looking to accelerate their coding however we are not
taking any responsibility for the success or failure you achieve using it.

**A tutorial on how to use this library is a part of our Project Orion training available now at https://mineacademy.org**
