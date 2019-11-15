import BaseNode from './BaseNode'
// 文本类型节点 分纯文本和图片文字(由客户端处理)
export default class TextNode extends BaseNode {
    textType // 文本类型 plain image
    content // 文本内容
    constructor(props) {
        super(props)
        this.nodeType = 'text'
        this.textType = 'plain'
        this.content = props && props.content
    }
}