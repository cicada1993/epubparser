export default class Chapter {
    bookKey
    // 章节编号
    chapterIndex
    // 渲染列表
    renderBlocks
    constructor(props) {
        this.bookKey = props && props.bookKey
        this.chapterIndex = props && props.chapterIndex
        this.renderBlocks = props && props.renderBlocks
    }
}