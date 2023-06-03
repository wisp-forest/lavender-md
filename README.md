## lavender-md

lavender-md is a highly extensible Markdown parsing & rendering engine for Minecraft. It implements the majority of what
you would expect from the average Markdown processor, with Discord-specific syntax for underscore and strikethrough, and 
some Minecraft-specific elements like `<entity; entity id here >` and `<block; block id here>`.

The library is distributed as two modules:
 - `core` contains the actual engine along with a standard set of features and a compiler that renders to Minecraft's text format
 - `owo-ui` contains the aforementioned Minecraft-specific features implemented through [owo-ui](https://github.com/wisp-forest/owo-lib)
   components alongside a compiler that produces an owo-ui component tree to be placed in an existing owo-powered environment or embedded 
   through owo-ui's strong embedding capabilities

**Build Setup:**
```properties
# https://maven.wispforest.io/io/wispforest/lavender-md/core/
lavender_md_version=...
```

```groovy
repositories {
    maven { url 'https://maven.wispforest.io' }
}

<...>

dependencies {
    modImplementation "io.wispforest.lavender-md:core:${project.lavender_md_version}"

    // if you plan to use the owo-ui dependent features
    modImplementation "io.wispforest.lavender-md:owo-ui:${project.lavender_md_version}"
}
```

## Getting Started

In order to render Markdown with lavender-md, you will generally use a `MarkdownProcessor` with a certain set of 
`MarkdownFeature`s. Of course, you can always dive deeper and use the low-level `Lexer` & `Parser` implementation 
separately and potentially do extensive transformations on the AST produced the parser, although that is rarely necessary.

To obtain a basic pipeline and use it to format some text (for example to be displayed in in-game chat), you 
can use `MarkdownProcessor.text()` like so:
```java
var processor = MarkdownProcessor.text();
var formatted = processor.process("**bold text**, with support for __underscores__ and {green}colors{}");
```

Do note that the `.text()` function generates a new instance every time you call it, so it is generally desirable to 
cache what it returns.

To add more features to an existing processor, copy it:
```java
var processor = MarkdownProcessor.text().copyWith(new LinkFeature());
var formatted = processor.process("this now does [links](https://wispforest.io)");
```

You can also change the compiler this way:
```java
var processor = MarkdownProcessor.text().copyWith(OwoUICompiler::new).copyWith(new EntityFeature());
var components = processor.process("this displays a zombie: <entity;minecraft:zombie>");
```

Finally, if you need maximal flexibility, you can also simply instantiate `MarkdownProcessor` directly.

## Writing extension
 - **Adding a new feature**
   Implement the `MarkdownFeature` interface and tokenize & parse to your heart's content. This can be somewhat tricky
   and requires at least a basic understanding of recursive-descent parsing. For basic, non-nested examples you can look
   the [owo-ui extension](https://github.com/wisp-forest/lavender-md/tree/master/owo-ui-extension/src/main/java/io/wispforest/lavendermd/feature) 
   for reference

 - **Adding a  custom compiler**<br>
   Implement the `MarkdownCompiler` interface and generate whatever output representation you need. Note that compilers
   should be stateful objects that get re-created every time the processor wants to process another batch of input. Also
   check out the `TextBuilder` class which takes care of generating styled Minecraft text objects (if you plan to use those)
