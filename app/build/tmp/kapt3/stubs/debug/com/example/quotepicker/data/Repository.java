package com.example.quotepicker.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\\\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0005\u0018\u0000 \'2\u00020\u0001:\u0001\'B\u000f\b\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\u0086@\u00a2\u0006\u0002\u0010\u000fJ&\u0010\u0010\u001a\u00020\f2\u0006\u0010\u0011\u001a\u00020\f2\u0006\u0010\u0012\u001a\u00020\u000e2\u0006\u0010\u0013\u001a\u00020\u0014H\u0086@\u00a2\u0006\u0002\u0010\u0015J&\u0010\u0016\u001a\u00020\f2\u0006\u0010\u0011\u001a\u00020\f2\u0006\u0010\u0017\u001a\u00020\u000e2\u0006\u0010\u0013\u001a\u00020\u0014H\u0086@\u00a2\u0006\u0002\u0010\u0015J\u0016\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u001bH\u0086@\u00a2\u0006\u0002\u0010\u001cJ\u0016\u0010\u001d\u001a\u00020\u00192\u0006\u0010\u001e\u001a\u00020\u001fH\u0086@\u00a2\u0006\u0002\u0010 J\u0012\u0010!\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001b0#0\"J!\u0010$\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001f0#0\"2\b\u0010\u0011\u001a\u0004\u0018\u00010\f\u00a2\u0006\u0002\u0010%J\u0016\u0010&\u001a\u00020\u00192\u0006\u0010\u001e\u001a\u00020\u001fH\u0086@\u00a2\u0006\u0002\u0010 R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006("}, d2 = {"Lcom/example/quotepicker/data/Repository;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "db", "Lcom/example/quotepicker/data/AppDatabase;", "groupDao", "Lcom/example/quotepicker/data/GroupDao;", "quoteDao", "Lcom/example/quotepicker/data/QuoteDao;", "addGroup", "", "name", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "addImageQuote", "groupId", "base64", "weight", "", "(JLjava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "addTextQuote", "text", "deleteGroup", "", "group", "Lcom/example/quotepicker/data/GroupEntity;", "(Lcom/example/quotepicker/data/GroupEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteQuote", "q", "Lcom/example/quotepicker/data/QuoteEntity;", "(Lcom/example/quotepicker/data/QuoteEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observeGroups", "Lkotlinx/coroutines/flow/Flow;", "", "observeQuotes", "(Ljava/lang/Long;)Lkotlinx/coroutines/flow/Flow;", "updateQuote", "Companion", "app_debug"})
public final class Repository {
    @org.jetbrains.annotations.NotNull()
    private final com.example.quotepicker.data.AppDatabase db = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.quotepicker.data.GroupDao groupDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.quotepicker.data.QuoteDao quoteDao = null;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private static volatile com.example.quotepicker.data.Repository INSTANCE;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.quotepicker.data.Repository.Companion Companion = null;
    
    private Repository(android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.example.quotepicker.data.GroupEntity>> observeGroups() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.example.quotepicker.data.QuoteEntity>> observeQuotes(@org.jetbrains.annotations.Nullable()
    java.lang.Long groupId) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object addGroup(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object deleteGroup(@org.jetbrains.annotations.NotNull()
    com.example.quotepicker.data.GroupEntity group, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object addTextQuote(long groupId, @org.jetbrains.annotations.NotNull()
    java.lang.String text, int weight, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object addImageQuote(long groupId, @org.jetbrains.annotations.NotNull()
    java.lang.String base64, int weight, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object updateQuote(@org.jetbrains.annotations.NotNull()
    com.example.quotepicker.data.QuoteEntity q, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object deleteQuote(@org.jetbrains.annotations.NotNull()
    com.example.quotepicker.data.QuoteEntity q, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u0007R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lcom/example/quotepicker/data/Repository$Companion;", "", "()V", "INSTANCE", "Lcom/example/quotepicker/data/Repository;", "get", "context", "Landroid/content/Context;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.quotepicker.data.Repository get(@org.jetbrains.annotations.NotNull()
        android.content.Context context) {
            return null;
        }
    }
}