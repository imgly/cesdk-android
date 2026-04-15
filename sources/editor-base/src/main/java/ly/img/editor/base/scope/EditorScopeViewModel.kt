package ly.img.editor.base.scope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

internal class EditorScopeViewModel :
    ViewModel(),
    ViewModelStoreOwner {
    override val viewModelStore by lazy { ViewModelStore() }

    override fun onCleared() {
        clear()
    }

    fun clear() {
        viewModelStore.clear()
    }
}
