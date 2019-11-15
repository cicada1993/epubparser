export default class RenderBlock {
    // 节点编号
    blockIndex
    // 根节点 一般是ContainerNode
    blockNode
    constructor(props) {
        this.blockIndex = props && props.blockIndex
        this.blockNode = props && props.blockNode
    }
}