package com.example.quotepicker.vm;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000v\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0014J\u001e\u0010\u0015\u001a\u00020\u00122\u0006\u0010\u0016\u001a\u00020\u00072\u0006\u0010\u0017\u001a\u00020\u00142\u0006\u0010\u0018\u001a\u00020\u0019J\u001e\u0010\u001a\u001a\u00020\u00122\u0006\u0010\u0016\u001a\u00020\u00072\u0006\u0010\u001b\u001a\u00020\u00142\u0006\u0010\u0018\u001a\u00020\u0019J\u0006\u0010\u001c\u001a\u00020\u001dJ\u000e\u0010\u001e\u001a\u00020\u001f2\u0006\u0010 \u001a\u00020\u0014J\u000e\u0010!\u001a\u00020\u00122\u0006\u0010\"\u001a\u00020#J\u000e\u0010$\u001a\u00020\u00122\u0006\u0010%\u001a\u00020\tJ\u000e\u0010&\u001a\u00020\u00142\u0006\u0010\'\u001a\u00020(J\u0006\u0010)\u001a\u00020\u0012J\u0015\u0010*\u001a\u00020\u001d2\b\u0010+\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\u0002\u0010,J\u000e\u0010-\u001a\u00020\u00122\u0006\u0010%\u001a\u00020\tJ7\u0010.\u001a\u0004\u0018\u0001H/\"\u0004\b\u0000\u0010/2\f\u00100\u001a\b\u0012\u0004\u0012\u0002H/012\u0012\u00102\u001a\u000e\u0012\u0004\u0012\u0002H/\u0012\u0004\u0012\u00020\u001903H\u0002\u00a2\u0006\u0002\u00104R\u0016\u0010\u0005\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\b\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\t0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010\u00a8\u00065"}, d2 = {"Lcom/example/quotepicker/vm/MainViewModel;", "Landroidx/lifecycle/AndroidViewModel;", "app", "Landroid/app/Application;", "(Landroid/app/Application;)V", "_currentGroupId", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_randomResult", "Lcom/example/quotepicker/data/QuoteEntity;", "repo", "Lcom/example/quotepicker/data/Repository;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "Lcom/example/quotepicker/vm/UiState;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "addGroup", "Lkotlinx/coroutines/Job;", "name", "", "addImageQuote", "groupId", "base64", "weight", "", "addTextQuote", "text", "clearRandom", "", "decodeBase64ToBitmap", "Landroid/graphics/Bitmap;", "b64", "deleteGroup", "group", "Lcom/example/quotepicker/data/GroupEntity;", "deleteQuote", "q", "encodeImageToBase64", "uri", "Landroid/net/Uri;", "pickRandom", "setGroup", "id", "(Ljava/lang/Long;)V", "updateQuote", "weightedPick", "T", "items", "", "weightOf", "Lkotlin/Function1;", "(Ljava/util/List;Lkotlin/jvm/functions/Function1;)Ljava/lang/Object;", "app_debug"})
public final class MainViewModel extends androidx.lifecycle.AndroidViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.example.quotepicker.data.Repository repo = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Long> _currentGroupId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.example.quotepicker.data.QuoteEntity> _randomResult = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.example.quotepicker.vm.UiState> uiState = null;
    
    public MainViewModel(@org.jetbrains.annotations.NotNull()
    android.app.Application app) {
        super(null);
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.example.quotepicker.vm.UiState> getUiState() {
        return null;
    }
    
    public final void setGroup(@org.jetbrains.annotations.Nullable()
    java.lang.Long id) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.Job addGroup(@org.jetbrains.annotations.NotNull()
    java.lang.String name) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.Job deleteGroup(@org.jetbrains.annotations.NotNull()
    com.example.quotepicker.data.GroupEntity group) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.Job addTextQuote(long groupId, @org.jetbrains.annotations.NotNull()
    java.lang.String text, int weight) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.Job addImageQuote(long groupId, @org.jetbrains.annotations.NotNull()
    java.lang.String base64, int weight) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.Job deleteQuote(@org.jetbrains.annotations.NotNull()
    com.example.quotepicker.data.QuoteEntity q) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.Job updateQuote(@org.jetbrains.annotations.NotNull()
    com.example.quotepicker.data.QuoteEntity q) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.Job pickRandom() {
        return null;
    }
    
    public final void clearRandom() {
    }
    
    private final <T extends java.lang.Object>T weightedPick(java.util.List<? extends T> items, kotlin.jvm.functions.Function1<? super T, java.lang.Integer> weightOf) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String encodeImageToBase64(@org.jetbrains.annotations.NotNull()
    android.net.Uri uri) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final android.graphics.Bitmap decodeBase64ToBitmap(@org.jetbrains.annotations.NotNull()
    java.lang.String b64) {
        return null;
    }
}