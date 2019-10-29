import RenderItem from './RenderItem'
export default class ImageItem extends RenderItem {
    src
    active
    alt
    constructor(props) {
        super(props)
        this.itemType = 'image'
        this.src = props && props.src
        this.active = props && props.active
        this.alt = props && props.alt
    }
}