import RenderItem from './RenderItem'
export default class ContainerItem extends RenderItem {
    children
    constructor(props) {
        super(props)
        this.itemType = 'container'
        this.children = props && props.children
    }
}