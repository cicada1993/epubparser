import Resource from './Resource'
// html资源文件
export default class HtmlResource extends Resource {
    htmlNode // html 对应的ReactNode
    renderBlocks // html 对应的段落列表
    constructor(props) {
        super(props)
        this.htmlNode = props && props.htmlNode
        this.renderBlocks = props && props.renderBlocks
    }
}