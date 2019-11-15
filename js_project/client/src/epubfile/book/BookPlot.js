export default class BookPlot {
    bookKey
    bookUrl
    zipped
    chapterIndex

    constructor(props) {
        this.bookKey = props && props.bookKey
        this.bookUrl = props && props.bookUrl
        this.zipped = props && props.zipped
        this.chapterIndex = props && props.chapterIndex
    }
}