import BaseNode from './BaseNode'
// 容器类型节点 可以包含子节点
export default class ContainerNode extends BaseNode {
    // 子节点列表
    children
    constructor(props){
        super(props)
        this.nodeType = 'container'
        this.children = props && props.children
    }
}