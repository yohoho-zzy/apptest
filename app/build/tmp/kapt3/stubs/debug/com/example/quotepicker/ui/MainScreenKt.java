package com.example.quotepicker.ui;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000B\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a*\u0010\u0000\u001a\u00020\u00012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u001a;\u0010\u0007\u001a\u00020\u00012\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\n0\t2\b\u0010\u000b\u001a\u0004\u0018\u00010\f2\u0014\u0010\r\u001a\u0010\u0012\u0006\u0012\u0004\u0018\u00010\f\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u00a2\u0006\u0002\u0010\u000e\u001a\u0012\u0010\u000f\u001a\u00020\u00012\b\b\u0002\u0010\u0010\u001a\u00020\u0011H\u0007\u001a>\u0010\u0012\u001a\u00020\u00012\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00140\t2\u0012\u0010\u0015\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00160\u00052\u0012\u0010\u0017\u001a\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u00a8\u0006\u0018"}, d2 = {"AddGroupDialog", "", "onDismiss", "Lkotlin/Function0;", "onConfirm", "Lkotlin/Function1;", "", "GroupTabs", "groups", "", "Lcom/example/quotepicker/data/GroupEntity;", "current", "", "onSelect", "(Ljava/util/List;Ljava/lang/Long;Lkotlin/jvm/functions/Function1;)V", "MainScreen", "vm", "Lcom/example/quotepicker/vm/MainViewModel;", "QuoteList", "quotes", "Lcom/example/quotepicker/data/QuoteEntity;", "decodeImage", "Landroid/graphics/Bitmap;", "onDelete", "app_debug"})
public final class MainScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void MainScreen(@org.jetbrains.annotations.NotNull()
    com.example.quotepicker.vm.MainViewModel vm) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void GroupTabs(java.util.List<com.example.quotepicker.data.GroupEntity> groups, java.lang.Long current, kotlin.jvm.functions.Function1<? super java.lang.Long, kotlin.Unit> onSelect) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void QuoteList(java.util.List<com.example.quotepicker.data.QuoteEntity> quotes, kotlin.jvm.functions.Function1<? super java.lang.String, android.graphics.Bitmap> decodeImage, kotlin.jvm.functions.Function1<? super com.example.quotepicker.data.QuoteEntity, kotlin.Unit> onDelete) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void AddGroupDialog(kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onConfirm) {
    }
}