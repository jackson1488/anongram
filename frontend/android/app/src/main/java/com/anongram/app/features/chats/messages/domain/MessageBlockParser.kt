package com.anongram.app.features.chats.messages.domain

import com.anongram.app.features.chats.ui.input.editor.CodeLanguages

/**
 * Последовательный парсер форматированного текста сообщения в цепочку блоков [MessageBlock].
 * Обеспечивает полноценное комбинирование любых типов блоков в одном сообщении
 * (Заголовки, Абзацы с инлайн-стилями, Сворачиваемые цитаты Telegram, Чек-листы, Код, Формулы).
 */
object MessageBlockParser {

    fun parse(rawText: String): List<MessageBlock> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return emptyList()

        // 1. Быстрая проверка на одиночный цельный блок кода на всё сообщение
        if (trimmed.startsWith("```") && trimmed.endsWith("```") && trimmed.length > 6) {
            val (lang, code) = parseCodeBlockContent(trimmed)
            return listOf(MessageBlock.Code(language = lang, code = code))
        }

        // 2. Быстрая проверка на одиночную формулу LaTeX на всё сообщение
        if ((trimmed.startsWith("\\(") && trimmed.endsWith("\\)")) ||
            (trimmed.startsWith("$$") && trimmed.endsWith("$$"))
        ) {
            return listOf(MessageBlock.Latex(formula = trimmed))
        }

        val lines = rawText.lines()
        val blocks = mutableListOf<MessageBlock>()
        var idx = 0
        var checklistCounter = 0

        while (idx < lines.size) {
            val line = lines[idx]
            val t = line.trim()

            // Пропускаем пустые строки между блоками, если нет накопленного контекста
            if (t.isEmpty()) {
                idx++
                continue
            }

            // 1. Блок кода с ```
            if (t.startsWith("```")) {
                val codeLang = t.removePrefix("```").trim().ifEmpty { "CODE" }
                val codeLines = mutableListOf<String>()
                idx++
                while (idx < lines.size && !lines[idx].trim().startsWith("```")) {
                    codeLines.add(lines[idx])
                    idx++
                }
                if (idx < lines.size && lines[idx].trim().startsWith("```")) {
                    idx++ // пропускаем закрывающий ```
                }
                blocks.add(MessageBlock.Code(language = codeLang, code = codeLines.joinToString("\n")))
                continue
            }

            // 1b. Блок кода без ``` но начинающийся с названия языка (напр. "kotlin\n...")
            if (idx == 0 && lines.size > 1 && CodeLanguages.any { t.equals(it, ignoreCase = true) }) {
                val matchedLang = CodeLanguages.first { t.equals(it, ignoreCase = true) }
                val codeContent = lines.drop(1).joinToString("\n")
                blocks.add(MessageBlock.Code(language = matchedLang, code = codeContent))
                break
            }

            // 2. Формула LaTeX
            if ((t.startsWith("\\(") && t.endsWith("\\)")) || (t.startsWith("$$") && t.endsWith("$$"))) {
                blocks.add(MessageBlock.Latex(formula = t))
                idx++
                continue
            }

            // 3. Заголовки #{1,6}
            val headingMatch = Regex("^(#{1,6})\\s+(.+)$").find(t)
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                val title = headingMatch.groupValues[2].trim()
                blocks.add(MessageBlock.Heading(level = level, text = title))
                idx++
                continue
            }

            // 4. Сворачиваемый блок со значком ^ (^ Заголовок\nТело)
            if (t.startsWith("^")) {
                val title = t.removePrefix("^").trim()
                val bodyLines = mutableListOf<String>()
                idx++
                while (idx < lines.size && !isNewBlockStart(lines[idx])) {
                    bodyLines.add(lines[idx])
                    idx++
                }
                val (bodyText, author) = extractAuthorFromLines(bodyLines)
                blocks.add(
                    MessageBlock.Collapsible(
                        title = title.ifEmpty { "Сворачиваемый блок" },
                        content = bodyText,
                        author = author
                    )
                )
                continue
            }

            // 4b. Сворачиваемый блок +++ Заголовок\nТело\n+++
            if (t.startsWith("+++")) {
                val title = t.removePrefix("+++").trim()
                val bodyLines = mutableListOf<String>()
                idx++
                while (idx < lines.size && !lines[idx].trim().startsWith("+++")) {
                    bodyLines.add(lines[idx])
                    idx++
                }
                if (idx < lines.size && lines[idx].trim().startsWith("+++")) {
                    idx++
                }
                val (bodyText, author) = extractAuthorFromLines(bodyLines)
                blocks.add(
                    MessageBlock.Collapsible(
                        title = title.ifEmpty { "Сворачиваемый блок" },
                        content = bodyText,
                        author = author
                    )
                )
                continue
            }

            // 5. Цитата (обычная > или сворачиваемая >>>)
            if (t.startsWith(">")) {
                val isCollapsible = t.startsWith(">>>")
                val quoteLines = mutableListOf<String>()

                while (idx < lines.size) {
                    val qLine = lines[idx]
                    val qTrim = qLine.trim()
                    if (qTrim.isEmpty()) break
                    if (qTrim.startsWith(">>>")) {
                        quoteLines.add(qTrim.removePrefix(">>>").trim())
                    } else if (qTrim.startsWith(">")) {
                        quoteLines.add(qTrim.removePrefix(">").trim())
                    } else if (qLine.startsWith(" ") || qLine.startsWith("\t")) {
                        quoteLines.add(qTrim)
                    } else {
                        break
                    }
                    idx++
                }

                val (quoteText, author) = extractAuthorFromLines(quoteLines)
                blocks.add(
                    MessageBlock.Quote(
                        text = quoteText,
                        author = author,
                        isCollapsible = isCollapsible
                    )
                )
                continue
            }

            // 6. Интерактивный чек-лист
            if (isChecklistLine(t)) {
                val items = mutableListOf<ChecklistItem>()
                while (idx < lines.size && isChecklistLine(lines[idx].trim())) {
                    val curLine = lines[idx].trim()
                    val (isChecked, itemText) = parseChecklistItem(curLine)
                    items.add(
                        ChecklistItem(
                            id = checklistCounter++,
                            isChecked = isChecked,
                            text = itemText
                        )
                    )
                    idx++
                }
                blocks.add(MessageBlock.Checklist(items = items))
                continue
            }

            // 7. Обычный абзац текста (параграф)
            val paragraphLines = mutableListOf<String>()
            while (idx < lines.size) {
                val cur = lines[idx]
                val curTrim = cur.trim()
                if (curTrim.isEmpty()) {
                    idx++
                    break
                }
                if (isNewBlockStart(cur)) {
                    break
                }
                paragraphLines.add(cur)
                idx++
            }

            if (paragraphLines.isNotEmpty()) {
                blocks.add(MessageBlock.Paragraph(text = paragraphLines.joinToString("\n")))
            }
        }

        return blocks
    }

    private fun isNewBlockStart(line: String): Boolean {
        val t = line.trim()
        if (t.startsWith("```")) return true
        if (t.startsWith("+++")) return true
        if (t.startsWith("^")) return true
        if (t.startsWith(">")) return true
        if (isChecklistLine(t)) return true
        if (Regex("^(#{1,6})\\s+").containsMatchIn(t)) return true
        if ((t.startsWith("\\(") && t.endsWith("\\)")) || (t.startsWith("$$") && t.endsWith("$$"))) return true
        return false
    }

    private fun isChecklistLine(line: String): Boolean {
        return line.startsWith("☐") || line.startsWith("☑") ||
            line.startsWith("- [ ]") || line.startsWith("- [x]", ignoreCase = true) ||
            line.startsWith("[ ]") || line.startsWith("[x]", ignoreCase = true)
    }

    private fun parseChecklistItem(line: String): Pair<Boolean, String> {
        return when {
            line.startsWith("☑") -> Pair(true, line.substring(1).trim())
            line.startsWith("☐") -> Pair(false, line.substring(1).trim())
            line.startsWith("- [x]", ignoreCase = true) -> Pair(true, line.substring(5).trim())
            line.startsWith("- [ ]") -> Pair(false, line.substring(5).trim())
            line.startsWith("[x]", ignoreCase = true) -> Pair(true, line.substring(3).trim())
            line.startsWith("[ ]") -> Pair(false, line.substring(3).trim())
            else -> Pair(false, line)
        }
    }

    private fun extractAuthorFromLines(lines: List<String>): Pair<String, String?> {
        val lastLine = lines.lastOrNull()?.trim() ?: return Pair("", null)
        val isAuthor = lastLine.startsWith("—") || lastLine.startsWith("--") ||
            (lastLine.startsWith("- ") && !lastLine.startsWith("- ["))
        if (isAuthor) {
            val author = lastLine.removePrefix("—").removePrefix("--").removePrefix("-").trim()
            val text = lines.dropLast(1).joinToString("\n").trim()
            return Pair(text, author.ifEmpty { null })
        }
        return Pair(lines.joinToString("\n").trim(), null)
    }

    private fun parseCodeBlockContent(raw: String): Pair<String, String> {
        val withoutTicks = raw.removePrefix("```")
        val firstLine = withoutTicks.substringBefore('\n').trim()
        val lang = if (firstLine.isNotBlank() && firstLine.length <= 15) firstLine else "CODE"
        val body = withoutTicks.substringAfter('\n').removeSuffix("```").trim()
        return lang to body
    }
}
