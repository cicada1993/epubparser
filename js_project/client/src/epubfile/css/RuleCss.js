import DeclCss from './DeclCss'
// Represents a CSS rule: a selector followed by a declaration block
export default class RuleCss {
    selector
    nodes
    constructor(props) {
        this.selector = props && props.selector
        const declNodes = props && props.nodes
        if (Array.isArray(declNodes)) {
            this.nodes = Array.of()
            for (let node of declNodes) {
                this.nodes.push(new DeclCss(node))
            }
        }
    }
}