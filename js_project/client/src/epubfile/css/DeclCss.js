// Represents a CSS declaration.
export default class DeclCss {
    prop
    value
    constructor(props) {
        this.prop = props && props.prop
        this.value = props && props.value
    }
}