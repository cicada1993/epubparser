import BaseNode from './BaseNode'
// 图片类型节点
export default class ImageNode extends BaseNode {
    src
    active
    alt
    href
    base64Data
    width
    height
    constructor(props) {
        super(props)
        this.nodeType = 'image'
        this.src = props && props.src
        this.active = props && props.active
        this.alt = props && props.alt
        this.href = props && props.href
        this.base64Data = props && props.base64Data
        this.width = props && props.width
        this.height = props && props.height
    }
}