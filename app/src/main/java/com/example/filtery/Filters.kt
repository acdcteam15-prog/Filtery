package com.example.filtery

/** The whole filter list. Funny ones come first. */
object Filters {
    val original = Fx("original", "Original", Cat.COLOR, "🙂", false) { }

    val all: List<Fx> by lazy {
        val l = ArrayList<Fx>()
        l.addFaceFilters()
        l.addFaceFilters2()
        l.addWarpFilters()
        l.addFxFilters()
        l.addColorFilters()
        l.addArtFilters()
        l.addFrameFilters()
        l.add(0, original)
        l
    }

    private val index: Map<String, Fx> by lazy { all.associateBy { it.id } }

    fun byId(id: String?): Fx = if (id == null) original else index[id] ?: original

    fun inCat(c: Cat): List<Fx> = all.filter { it.cat == c && it !== original }

    /** Number of real filters (not counting "Original"). */
    val count: Int get() = all.size - 1
}
