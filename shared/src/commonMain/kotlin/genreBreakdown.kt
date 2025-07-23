fun <E> genreBreakdown(
    tracklist: Tracklist<E>,
    getGenre: E.() -> String?
) {
    if(tracklist.tracks.all { it.getGenre() == null }) return

    val genreCount = tracklist.tracks.groupingBy { it.getGenre() }.eachCount()
    println("")
    println(tracklist.title)
    println("GENRES: ")
    genreCount.entries.sortedByDescending { it.value }
        .forEach {
            println("${it.value} x ${it.key}")
        }
}