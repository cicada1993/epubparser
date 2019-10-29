import RenderItem from './RenderItem'
export default class TextItem extends RenderItem {
    content
    constructor(props) {
        super(props)
        this.itemType = 'text'
        this.content = props && props.content
    }
}