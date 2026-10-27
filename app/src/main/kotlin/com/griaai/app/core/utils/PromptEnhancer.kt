package com.griaai.app.core.utils

/**
 * 100% Offline Prompt Auto-Enhancer.
 * Transforms brief, ambiguous user queries into structured, high-yield system-grade prompts.
 */
object PromptEnhancer {

    fun enhance(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return trimmed

        val lower = trimmed.lowercase()

        // 1. Coding / Development Queries
        val isCode = lower.contains("code") || lower.contains("script") ||
                lower.contains("function") || lower.contains("python") ||
                lower.contains("kotlin") || lower.contains("javascript") ||
                lower.contains("typescript") || lower.contains("java") ||
                lower.contains("c++") || lower.contains("rust") ||
                lower.contains("sql") || lower.contains("api") ||
                lower.contains("html") || lower.contains("css") ||
                lower.contains("app") || lower.contains("bug") ||
                lower.contains("algorithm")

        if (isCode) {
            return buildString {
                append("Act as a staff-level software engineer. ")
                append(trimmed.replaceFirstChar { it.uppercase() })
                if (!trimmed.endsWith(".")) append(".")
                append("\n\n")
                append("Please ensure the solution includes:\n")
                append("1. **Idiomatic, production-grade code** with complete error handling and comments.\n")
                append("2. **Key architectural trade-offs** and complexity considerations (time & space).\n")
                append("3. **Edge cases** handled gracefully (e.g., null values, network failures, bounds).\n")
                append("4. A brief, executable **example or unit test** verifying correctness.")
            }
        }

        // 2. Explanation / Learning Queries
        val isExplain = lower.startsWith("explain") || lower.startsWith("what is") ||
                lower.startsWith("how does") || lower.startsWith("why") ||
                lower.contains("teach me") || lower.contains("difference between")

        if (isExplain) {
            return buildString {
                append("Provide a clear, in-depth explanation of: ")
                append(trimmed.removePrefix("explain").removePrefix("Explain").trim())
                if (!trimmed.endsWith(".")) append(".")
                append("\n\n")
                append("Structure your answer as follows:\n")
                append("1. **High-Level Summary / TL;DR**: One intuitive paragraph.\n")
                append("2. **Core Mechanics & Fundamentals**: How it works under the hood with an intuitive analogy.\n")
                append("3. **Practical Real-World Example**: A concrete scenario or use case.\n")
                append("4. **Common Pitfalls & Misconceptions**: Key caveats to be aware of.")
            }
        }

        // 3. Summarization / Document Queries
        val isSummary = lower.contains("summarize") || lower.contains("tldr") || lower.contains("digest")
        if (isSummary) {
            return buildString {
                append("Provide a structured executive summary of: ")
                append(trimmed.removePrefix("summarize").removePrefix("Summarize").trim())
                if (!trimmed.endsWith(".")) append(".")
                append("\n\n")
                append("Format requirements:\n")
                append("• **Key Takeaways**: 3 to 5 high-impact bullet points.\n")
                append("• **Critical Insights & Decisions**: Actionable conclusions.\n")
                append("• **Omit conversational filler** and focus purely on facts.")
            }
        }

        // 4. General Enhancement
        return buildString {
            append("Please provide a thorough, structured, and insightful response to: ")
            append(trimmed.replaceFirstChar { it.uppercase() })
            if (!trimmed.endsWith(".")) append(".")
            append("\n\n")
            append("Ensure your response is clear, actionable, well-formatted with markdown headings, and highlights any important nuances or practical tips.")
        }
    }
}

