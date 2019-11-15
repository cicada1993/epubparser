import Resource from './Resource'
// css资源文件
export default class CssResource extends Resource {
    rules // css样式列表
    constructor(props) {
        super(props)
        this.rules = props && props.rules
    }
}