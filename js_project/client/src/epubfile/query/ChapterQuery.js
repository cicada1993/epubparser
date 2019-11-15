export default class ChapterQuery {
    chapterRequest
    state
    hitType

    constructor(props) {
        this.chapterRequest = props && props.chapterRequest
    }

    isSame(other) {
        return this.chapterRequest.isSame(other.chapterRequest)
    }

    getBookKey() {
        return this.chapterRequest && this.chapterRequest.bookKey
    }

    getChapterIndex() {
        return this.chapterRequest && this.chapterRequest.chapterIndex
    }
}