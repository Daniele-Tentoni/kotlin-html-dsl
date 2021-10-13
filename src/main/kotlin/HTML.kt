package io.github.danieletentoni

interface Element {
    fun render(indent: String = ""): String
}

interface RepeatableElement : Element

typealias Attribute = Pair<String, String>

typealias Attributes = Map<String, String>

interface Tag : Element {
    val name: String
    val children: List<Element>
    val attributes: Attributes
}

abstract class AbstractTag(override val name: String, vararg attributes: Attribute) : Tag {
    final override var children: List<Element> = emptyList() // Override val with var
        private set(value) {
            field = value
        } // Access only from this class

    final override val attributes: Attributes = attributes.associate { it }

    fun registerElement(element: Element) {
        if (element is RepeatableElement || children.none { it::class == element::class }) {
            children = children + element
        } else {
            throw IllegalStateException("cannot repeat tag ${element::class.simpleName} multiple times:\n$element")
        }
    }

    final override fun render(indent: String): String =
        """
            |$indent<$name${renderAttribute()}>
            |${renderChildren(indent + INDENT)}
            |$indent</$name>
        """
            .trimMargin()
            .replace("""\R+""".toRegex(), "\n")

    private fun renderChildren(indent: String): String =
        children.map { it.render(indent) }.joinToString(separator = "\n")

    private fun renderAttribute(): String = attributes.takeIf { it.isNotEmpty() }
        ?.map { (attribute, value) -> "$attribute=\"$value\"" }
        ?.joinToString(separator = " ", prefix = " ")
        ?: ""
}

interface RepeatableTag : Tag, RepeatableElement
interface TextElement : RepeatableElement {
    val text: String
    override fun render(indent: String) = "$indent$text\n"
}

const val INDENT = "\t"

data class Text(override val text: String) : TextElement

@HtmlTagMarker
class HTML(vararg attributes: Attribute = arrayOf()) : AbstractTag("html", *attributes) {
    fun head(init: Head.() -> Unit): Head =
        Head().apply(init).also { this.registerElement(it) }

    fun body(vararg attributes: Attribute, init: Body.() -> Unit): Body =
        Body(*attributes).apply(init).also { this.registerElement(it) }
}

fun html(vararg attributes: Attribute, init: HTML.() -> Unit): HTML = HTML(*attributes).apply(init)

@HtmlTagMarker
class Head : AbstractTag("head") {
    fun title(configuration: Title.() -> Unit = {}) = registerElement(Title().apply(configuration))
}

@HtmlTagMarker
abstract class TagWithText(name: String, vararg attributes: Attribute) : AbstractTag(name, *attributes) {
    operator fun String.unaryMinus() = registerElement(Text(this))
}

class Title : TagWithText("title")

class Body(vararg attributes: Attribute = arrayOf()) : TagWithText("body", *attributes)

const val newline = "<br />"

@DslMarker
annotation class HtmlTagMarker

fun main() {
    val document = html("lang" to "en") {
        head {
            title {
                -"An experiment"
            }
        }
        body {
            -"My Contents"
            -newline
            -"This contents are less important"
        }
    }.render()
    println(document)
}

/*
The goal

---

html {
    head {
        title { -"A link to the unibo webpage" }
    }
    body {
        p("class" to "myCustomCssClass") {
            a(href = "http://www.unibo.it") { -"Unibo Website" }
        }
    }
}.render()

to render

<html>
	<head>
		<title>
			A link to the unibo webpage
		</title>
	</head>
	<body>
		<p class="myCustomCssClass">
			<a href="http://www.unibo.it">
				Unibo Website
			</a>
		</p>
	</body>
</html>
*/