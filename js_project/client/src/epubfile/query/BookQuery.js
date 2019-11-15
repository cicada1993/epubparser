export default class BookQuery {
    bookRequest

    state // 查询状态
    hitType // 结果类型  'none' 未打开过 'cache' 缓存
    originBook // epub.js 解析得到的原始数据
    opfPackage // 重构后的书籍信息
    resource // 书籍资源数据
    constructor(props) {
        this.bookRequest = props && props.bookRequest
    }

    isSame(other) {
        return this.bookRequest && this.bookRequest.isSame(other.bookRequest)
    }

    getBookKey() {
        return this.bookRequest && this.bookRequest.bookKey
    }

    isBookOpen() {
        return this.originBook && this.originBook.isOpen
    }
}