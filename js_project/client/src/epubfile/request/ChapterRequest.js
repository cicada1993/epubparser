export default class ChapterRequest {
    bookKey
    bookUrl
    zipped
    chapterIndex
    backUrl

    constructor(props) {
        this.bookKey = props && props.bookKey
        this.bookUrl = props && props.bookUrl
        this.zipped = props && props.zipped
        this.chapterIndex = props && props.chapterIndex
        this.backUrl = props && props.backUrl
    }

    isSame(other) {
        const { bookKey, bookUrl, zipped, chapterIndex, backUrl } = other
        return (
            this.bookKey == bookKey &&
            this.bookUrl == bookUrl &&
            this.zipped == zipped &&
            this.chapterIndex == chapterIndex &&
            this.backUrl == backUrl
        )
    }
}