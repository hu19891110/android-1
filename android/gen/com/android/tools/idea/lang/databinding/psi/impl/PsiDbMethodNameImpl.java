// This is a generated file. Not intended for manual editing.
package com.android.tools.idea.lang.databinding.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.android.tools.idea.lang.databinding.psi.DbTokenTypes.*;
import com.android.tools.idea.lang.databinding.DataBindingPsiElement;
import com.android.tools.idea.lang.databinding.psi.*;

public class PsiDbMethodNameImpl extends DataBindingPsiElement implements PsiDbMethodName {

  public PsiDbMethodNameImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiDbVisitor visitor) {
    visitor.visitMethodName(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PsiDbVisitor) accept((PsiDbVisitor)visitor);
    else super.accept(visitor);
  }

}
