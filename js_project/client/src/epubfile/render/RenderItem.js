export default class RenderItem {
    itemType
    className
    style
    type
    constructor(props) {
        this.className = props && props.className
        this.style = props && props.style
        this.type = props && props.type
    }
}