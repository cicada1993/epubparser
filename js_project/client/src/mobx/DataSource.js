import { observer } from 'mobx-react'
import { decorate, observable, computed, autorun, action } from 'mobx'

class DataSource {
    mutabelContent = "fff"

    constructor() {

    }

    get content() {
        console.log('mutableContent is computed', this.mutabelContent)
        return this.mutabelContent
    }

    setContent(content) {
        this.mutabelContent = content
    }
}

decorate(DataSource, {
    mutabelContent: observable,
    content: computed,
    setContent: action
})

export default DataSource