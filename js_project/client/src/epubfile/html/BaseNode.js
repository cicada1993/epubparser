export default class BaseNode {
    nodeType
    type
    className
    style
    bookPlot
    constructor(props) {
        this.type = props && props.type
        this.className = props && props.className
        this.style = props && props.style
        this.bookPlot = props && props.bookPlot
    }
}