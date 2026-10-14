package app.leaf.reader.core.data.db

import app.leaf.reader.core.model.DocType

/**
 * The demo library, generated verbatim from `design/leaf-mockup-v2.html`
 * (SEED_DOCS / SEED_FOLDERS / SEED_TAGS / SEED_SMART) by
 * `tools/gen_seed_content.py`. Edit the mockup, re-run the generator.
 */
internal data class SeedBookmark(
    val page: Int,
    val label: String,
    val createdAt: Long
)

internal data class SeedDocument(
    val id: String,
    val name: String,
    val type: DocType,
    /** Size in megabytes, exactly as the mockup states it. */
    val sizeMb: Double,
    val dateAdded: Long,
    val lastOpened: Long,
    val folderId: String?,
    val tags: List<String>,
    val favorite: Boolean,
    val favoritedAt: Long?,
    /** Last page read, 0-based, as the mockup stores it. */
    val page: Int,
    val bookmarks: List<SeedBookmark>,
    /** One entry per page; each page is its paragraphs in reading order. */
    val pages: List<List<String>>
)

internal const val MINUTE_MS = 60_000L
internal const val HOUR_MS = 60 * MINUTE_MS
internal const val DAY_MS = 24 * HOUR_MS

/** Builds the demo documents relative to [now], as the mockup does. */
internal fun seedDocuments(now: Long): List<SeedDocument> = listOf(
    SeedDocument(
        id = "d1",
        name = "The Art of Simple Living.pdf",
        type = DocType.PDF,
        sizeMb = 2.4,
        dateAdded = now - 12*DAY_MS,
        lastOpened = now - 2*HOUR_MS,
        folderId = "f-person",
        tags = listOf("personal", "reading"),
        favorite = true,
        favoritedAt = now - 11*DAY_MS,
        page = 2,
        bookmarks = listOf(SeedBookmark(page = 1, label = "Chapter one", createdAt = now - 3*DAY_MS)),
        pages = listOf(
            listOf(
                "Simplicity is not about having less. It is about making room for what matters most. When we remove the unnecessary, the essential becomes visible.",
                "This book is a guide to slow, deliberate living. Each chapter offers one small practice you can try today, without buying anything and without changing your life overnight.",
                "The leaf does not hurry, yet everything is accomplished.",
            ),
            listOf(
                "Chapter One — The Quiet Morning",
                "Begin the day without a screen. Let the first hour belong to you and to the light coming through the window.",
                "A simple ritual — tea, a notebook, ten minutes of stillness — sets the tone for everything that follows. The practice is not the point. The attention is.",
                "Try it for seven days. Notice what changes.",
            ),
            listOf(
                "Chapter Two — One Surface",
                "Choose a single surface in your home and keep it clear. A desk, a table, a shelf. This is your quiet place.",
                "Clutter is postponed decisions. Every object on that surface is a question you have not answered yet. Answer it, and the surface stays clear.",
                "The leaf keeps only what it needs.",
            ),
            listOf(
                "Chapter Three — The Slow Reader",
                "Read one book at a time. Finish it before you begin another. This is harder than it sounds, and far more rewarding.",
                "Underline what moves you. Return to it. A book read slowly is worth ten books read quickly.",
                "The practice of reading is the practice of attention.",
            ),
            listOf(
                "Chapter Four — Digital Quiet",
                "Turn off every notification that is not a person. Your phone should not interrupt your life; it should serve it.",
                "Keep one screen for reading. Let it be calm. Let it remember where you stopped.",
                "A good tool disappears into the work.",
            ),
            listOf(
                "Chapter Five — Enough",
                "The question is not how much you can do, but how much is enough. Answer it honestly and the rest becomes simple.",
                "Enough is a decision, not an amount.",
                "The leaf falls when it is time, and not before.",
            ),
        )
    ),
    SeedDocument(
        id = "d2",
        name = "Q3 Product Roadmap.pdf",
        type = DocType.PDF,
        sizeMb = 1.1,
        dateAdded = now - 4*DAY_MS,
        lastOpened = now - 26*HOUR_MS,
        folderId = "f-rep",
        tags = listOf("work", "important"),
        favorite = true,
        favoritedAt = now - 3*DAY_MS,
        page = 0,
        bookmarks = listOf(),
        pages = listOf(
            listOf(
                "Q3 Product Roadmap — Confidential Draft",
                "This document outlines the product priorities for the third quarter. It is a working draft and will change as we learn.",
                "Three themes: retention, performance, and craft.",
            ),
            listOf(
                "Theme One — Retention",
                "Our month-over-month retention sits at 34%. The goal for Q3 is 45%.",
                "Key initiative: the resume-reading experience. Users who return to a document within 24 hours are three times more likely to still be active after a month.",
                "Owner: Maya. Milestone: end of week 4.",
            ),
            listOf(
                "Theme Two — Performance",
                "Cold start must be under 800ms on mid-range devices. We are currently at 1.4 seconds.",
                "Rendering large PDFs above 200 pages causes dropped frames. We will move page rendering to a background thread.",
                "Owner: Dmitri. Milestone: end of week 7.",
            ),
            listOf(
                "Theme Three — Craft",
                "Craft is the difference between a tool people use and a tool people love. We will spend 20% of the quarter on polish alone.",
                "Focus areas: typography, transitions, empty states, and haptics.",
                "Owner: the entire team. Milestone: continuous.",
            ),
            listOf(
                "Risks and Open Questions",
                "Hiring is behind plan by two engineers. If this does not change, we will cut the annotation feature from Q3.",
                "Open question: do we ship the tabbed reading experience in Q3 or Q4?",
                "Decision needed by Friday.",
            ),
        )
    ),
    SeedDocument(
        id = "d3",
        name = "Mediterranean Recipes.pdf",
        type = DocType.PDF,
        sizeMb = 4.8,
        dateAdded = now - 30*DAY_MS,
        lastOpened = now - 6*DAY_MS,
        folderId = "f-person",
        tags = listOf("recipe", "personal"),
        favorite = false,
        favoritedAt = null,
        page = 0,
        bookmarks = listOf(),
        pages = listOf(
            listOf(
                "A Small Book of Mediterranean Cooking",
                "These recipes come from a kitchen on the coast, where the market decides the menu and the menu changes daily.",
                "Everything here is simple. Few ingredients, treated well.",
            ),
            listOf(
                "Tomato and Bread",
                "Four ripe tomatoes, a loaf of yesterday's bread, one clove of garlic, good olive oil, salt.",
                "Rub the bread with garlic. Tear it. Toss with tomatoes, oil, and salt. Wait ten minutes. Eat.",
                "The quality of the olive oil is the whole recipe.",
            ),
            listOf(
                "Lemon and Olive Chicken",
                "Chicken thighs, preserved lemon, green olives, thyme, olive oil.",
                "Brown the chicken. Add everything else. Cover and cook slowly for forty minutes.",
                "Serve with bread to catch the juice.",
            ),
            listOf(
                "Fig and Honey",
                "Ripe figs, honey, sheep's milk yogurt, toasted walnuts.",
                "Split the figs. Warm them briefly. Spoon over the yogurt. Add honey and walnuts.",
                "Summer in a bowl.",
            ),
        )
    ),
    SeedDocument(
        id = "d4",
        name = "Clean Code — Reading Notes.docx",
        type = DocType.DOCX,
        sizeMb = 0.6,
        dateAdded = now - 8*DAY_MS,
        lastOpened = now - 3*DAY_MS,
        folderId = "f-uni",
        tags = listOf("study", "important"),
        favorite = true,
        favoritedAt = now - 8*DAY_MS,
        page = 3,
        bookmarks = listOf(SeedBookmark(page = 2, label = "Functions", createdAt = now - 2*DAY_MS)),
        pages = listOf(
            listOf(
                "Clean Code — Reading Notes",
                "Notes taken while reading. These are my own summaries and do not replace the book.",
                "Chapter 2: Meaningful Names.",
            ),
            listOf(
                "Meaningful Names",
                "A name should reveal intent. If a name requires a comment, then the name does not reveal its intent.",
                "Avoid disinformation. Avoid encodings. Avoid mental mapping.",
                "Class names are nouns. Method names are verbs.",
            ),
            listOf(
                "Functions",
                "Functions should be small. Then they should be smaller than that.",
                "A function should do one thing. It should do it well. It should do it only.",
                "The ideal number of arguments is zero, then one, then two. Three should be avoided.",
            ),
            listOf(
                "Comments",
                "Comments are always failures. We must have them because we cannot always figure out how to express ourselves without them.",
                "Explain why, not what. The code already says what.",
                "Delete commented-out code. It will live in version control.",
            ),
            listOf(
                "Formatting",
                "Code formatting is about communication, and communication is the professional developer's first order of business.",
                "Vertical openness between concepts. Vertical density between related lines.",
                "Teams should agree on a single formatting style and then follow it.",
            ),
            listOf(
                "Error Handling",
                "Use exceptions rather than return codes. Write your try-catch-finally statement first.",
                "Don't return null. Don't pass null.",
                "Define the normal flow, and handle the exceptional separately.",
            ),
            listOf(
                "Summary",
                "Clean code is not written by following a set of rules. You become a craftsman by first practicing the rules, then forgetting them.",
                "The ratio of time spent reading versus writing code is well over 10 to 1.",
            ),
        )
    ),
    SeedDocument(
        id = "d5",
        name = "User Research Findings.pdf",
        type = DocType.PDF,
        sizeMb = 3.2,
        dateAdded = now - 2*DAY_MS,
        lastOpened = now - 45*MINUTE_MS,
        folderId = "f-rep",
        tags = listOf("work"),
        favorite = false,
        favoritedAt = null,
        page = 1,
        bookmarks = listOf(SeedBookmark(page = 1, label = "Biggest frustration", createdAt = now - 1*DAY_MS)),
        pages = listOf(
            listOf(
                "User Research Findings — Spring Study",
                "Twelve interviews, four diary studies, and a survey of 1,240 users.",
                "This document summarizes the themes that appeared most often.",
            ),
            listOf(
                "Theme — Losing Your Place",
                "Nine of twelve participants described losing their place in long documents as their single biggest frustration.",
                "Quote: “I have a 300-page manual. Every time I open it I have to scroll and hunt. I just stopped using it.”",
                "This is our biggest opportunity.",
            ),
            listOf(
                "Theme — Too Many Files",
                "Users describe their downloads folder as a junk drawer. They cannot find what they saved.",
                "Quote: “I know I downloaded it. I just do not know where it went.”",
                "Folders alone are not enough. Users want to search by what the document is about.",
            ),
            listOf(
                "Theme — Reading at Night",
                "Seven of twelve read in bed. Blue light and harsh white backgrounds were mentioned repeatedly.",
                "Quote: “It hurts my eyes. I usually just give up and look at my phone instead.”",
                "A warm reading mode is not a nice-to-have. It is table stakes.",
            ),
            listOf(
                "Recommendations",
                "One: make resume-reading the center of the experience. Two: add tags and smart search. Three: ship a sepia and night reading mode.",
                "Four: keep the interface quiet. Users do not want a filing system. They want their document back.",
            ),
        )
    ),
    SeedDocument(
        id = "d6",
        name = "Kyoto — Five Days in April.txt",
        type = DocType.TXT,
        sizeMb = 0.02,
        dateAdded = now - 20*DAY_MS,
        lastOpened = now - 11*DAY_MS,
        folderId = "f-person",
        tags = listOf("travel", "personal"),
        favorite = false,
        favoritedAt = null,
        page = 0,
        bookmarks = listOf(),
        pages = listOf(
            listOf(
                "Kyoto — Five Days, April",
                "Day 1: Arrive, walk the Philosopher's Path, dinner near Gion.",
                "Day 2: Fushimi Inari at sunrise. Tea in Uji in the afternoon.",
                "Day 3: Arashiyama bamboo grove, then the monkey park.",
            ),
            listOf(
                "Day 4 and Day 5",
                "Day 4: Nara day trip. Deer, Todai-ji, the long walk back.",
                "Day 5: Nishiki Market, a last bowl of soba, the train to the airport.",
                "Notes: bring cash. Many small places do not take cards.",
            ),
            listOf(
                "Packing",
                "One bag. Two pairs of socks. A rain shell. A notebook and one pen.",
                "Leave room for nothing. You will buy a book anyway.",
            ),
        )
    ),
    SeedDocument(
        id = "d7",
        name = "Designing Data-Intensive Applications.pdf",
        type = DocType.PDF,
        sizeMb = 12.6,
        dateAdded = now - 1*DAY_MS,
        lastOpened = now - 20*HOUR_MS,
        folderId = "f-uni",
        tags = listOf("study"),
        favorite = false,
        favoritedAt = null,
        page = 0,
        bookmarks = listOf(),
        pages = listOf(
            listOf(
                "Chapter 1 — Reliable, Scalable, Maintainable",
                "The Internet was built without ideas of transactions and consistency — yet we expect both.",
                "Reliability means continuing to work correctly, even when things go wrong.",
                "Scalability describes a system's ability to cope with increased load.",
            ),
            listOf(
                "Thinking About Data Systems",
                "Many applications are data-intensive rather than compute-intensive: the bottleneck is data volume, complexity, and rate of change.",
                "There is no single tool that can do everything well. We compose several tools that work well together.",
                "The goal is the right tool for the right job.",
            ),
            listOf(
                "Load and Performance",
                "Load is described with a few numbers: requests per second, ratio of reads to writes, number of simultaneous users.",
                "When load increases, one of two things happens: performance is preserved and resources increase, or performance degrades.",
                "Describe performance with percentiles, not averages.",
            ),
        )
    ),
    SeedDocument(
        id = "d8",
        name = "Weekend Notes.txt",
        type = DocType.TXT,
        sizeMb = 0.01,
        dateAdded = now - 6*HOUR_MS,
        lastOpened = now - 6*HOUR_MS,
        folderId = null,
        tags = listOf(),
        favorite = false,
        favoritedAt = null,
        page = 0,
        bookmarks = listOf(),
        pages = listOf(
            listOf(
                "Weekend Notes",
                "A short list. Nothing urgent in it.",
                "Fix the bike light. Call Amara. Finish the last chapter of the green book.",
            ),
        )
    ),
    SeedDocument(
        id = "d9",
        name = "Q3 Budget — 2026.xlsx",
        type = DocType.XLSX,
        sizeMb = 0.9,
        dateAdded = now - 3*DAY_MS,
        lastOpened = now - 2*DAY_MS,
        folderId = "f-rep",
        tags = listOf("work", "important"),
        favorite = false,
        favoritedAt = null,
        page = 0,
        bookmarks = listOf(),
        pages = listOf(
            listOf(
                "Sheet 1 — Summary",
                "Q3 budget summary. All figures in thousands.",
                "Engineering 412 · Design 88 · Research 64 · Marketing 140 · Operations 96.",
                "Total planned spend 800. Contingency of 5% is held centrally.",
            ),
            listOf(
                "Sheet 2 — Engineering",
                "Headcount 14. Salaries 336. Tooling and licences 42. Cloud 34.",
                "Two open roles are budgeted from week six. Contract review costs 12.",
            ),
            listOf(
                "Sheet 3 — Marketing",
                "Launch campaign 74. Content production 26. Events 28. Sponsorship 12.",
                "Reallocation is possible if the launch slips past week nine.",
            ),
        )
    ),
    SeedDocument(
        id = "d10",
        name = "Q3 Kickoff Deck.pptx",
        type = DocType.PPTX,
        sizeMb = 5.4,
        dateAdded = now - 5*DAY_MS,
        lastOpened = now - 4*DAY_MS,
        folderId = "f-work",
        tags = listOf("work"),
        favorite = false,
        favoritedAt = null,
        page = 0,
        bookmarks = listOf(),
        pages = listOf(
            listOf(
                "Slide 1 — Title",
                "Q3 kickoff. Three themes: retention, performance, craft.",
                "Presented by the product team.",
            ),
            listOf(
                "Slide 2 — Why now",
                "Retention is flat at 34%. Cold start is 1.4 seconds. People read at night and dislike blue light.",
                "Competitors ship fast; we ship durable.",
            ),
            listOf(
                "Slide 3 — What we will not do",
                "No new platforms this quarter. No rewrite. Nothing that cannot be measured in a week.",
                "Focus is a decision about what to refuse.",
            ),
        )
    ),
)
