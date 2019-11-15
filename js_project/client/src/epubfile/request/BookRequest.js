export default class BookRequest {
    bookKey
    bookUrl
    zipped
    backUrl
    constructor(props) {
        this.bookKey = props && props.bookKey
        this.bookUrl = props && props.bookUrl
        this.zipped = props && props.zipped
        this.backUrl = props && props.backUrl
    }
    isSame(other) {
        const { bookKey, bookUrl, zipped, backUrl } = other
        return (
            this.bookKey == bookKey &&
            this.bookUrl == bookUrl &&
            this.zipped == zipped &&
            this.backUrl == backUrl
        )
    }
}