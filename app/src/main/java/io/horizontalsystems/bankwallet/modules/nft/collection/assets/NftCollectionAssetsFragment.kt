package io.horizontalsystems.bankwallet.modules.nft.collection.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.modules.nft.NftCollection
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModule
import io.horizontalsystems.bankwallet.modules.nft.collection.NftCollectionViewModel
import io.horizontalsystems.bankwallet.modules.nft.ui.NftAssetPreview
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSCircularProgressIndicator
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.core.findNavController

class NftCollectionAssetsFragment : BaseFragment() {
    private val viewModel by navGraphViewModels<NftCollectionViewModel>(R.id.nftCollectionFragment)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    NftCollectionAssetsScreen(findNavController(), viewModel.collection)
                }
            }
        }
    }
}

@Composable
fun NftCollectionAssetsScreen(navController: NavController, collection: NftCollection?) {
    if (collection == null) return

    val viewModel = viewModel<NftCollectionAssetsViewModel>(factory = NftCollectionAssetsModule.Factory(collection))

    Crossfade(viewModel.viewState) { viewState ->
        when (viewState) {
            ViewState.Loading -> {
                Loading()
            }
            is ViewState.Error -> {
                ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
            }
            ViewState.Success -> {
                NftAssets(navController, viewModel.assets, viewModel::onBottomReached, viewModel.loadingMore)
            }
        }
    }
}

@Composable
private fun NftAssets(
    navController: NavController,
    assets: List<CollectionAsset>?,
    onBottomReached: () -> Unit, loadingMore: Boolean
) {
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        assets?.chunked(2)?.forEachIndexed { index, assets ->
            item(key = "content-row-$index") {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    assets.forEach { collectionAsset ->
                        val asset = collectionAsset.asset
                        val price = collectionAsset.price
                        Box(modifier = Modifier.weight(1f)) {
                            NftAssetPreview(
                                name = asset.name,
                                imageUrl = asset.imageUrl,
                                onSale = asset.onSale,
                                tokenId = asset.tokenId,
                                coinPrice = price?.coinValue,
                                currencyPrice = price?.currencyValue
                            ) {
                                NftAssetModule.setParams(collectionAsset.asset)
                                navController.slideFromBottom(
                                    R.id.nftAssetFragment
                                )
                            }
                        }
                    }

                    if (assets.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(26.dp))
        }

        if (loadingMore) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    HSCircularProgressIndicator()
                }
            }
        }
    }

    listState.OnBottomReached(buffer = 6) {
        onBottomReached()
    }
}

@Composable
fun LazyListState.OnBottomReached(
    // tells how many items before we reach the bottom of the list
    // to call onLoadMore function
    buffer: Int = 0,
    onLoadMore: () -> Unit
) {
    // Buffer must be positive.
    // Or our list will never reach the bottom.
    require(buffer >= 0) { "buffer cannot be negative, but was $buffer" }

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf true

            // subtract buffer from the total items
            lastVisibleItem.index >= layoutInfo.totalItemsCount - 1 - buffer
        }
    }

    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore.value }
            .collect { if (it) onLoadMore() }
    }
}