/*
 * Copyright (c) 2020, Mulesoft, LLC. All rights reserved.
 * Use of this source code is governed by a BSD 3-Clause License
 * license that can be found in the LICENSE.txt file.
 */
package com.mulesoft.tools.migration.step;

import static com.google.common.base.Preconditions.checkArgument;

import com.mulesoft.tools.migration.exception.MigrationStepException;
import com.mulesoft.tools.migration.project.model.ApplicationModel;
import com.mulesoft.tools.migration.step.category.ApplicationModelContribution;
import com.mulesoft.tools.migration.step.category.MigrationReport;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic unit of execution.
 *
 * @author Mulesoft Inc.
 * @since 1.0.0
 */
public abstract class AbstractApplicationModelMigrationStep implements ApplicationModelContribution {

  private XPathExpression appliedTo;
  // TODO REMOVE
  private String originalXpathExpression;
  private ApplicationModel applicationModel;
  public List<Namespace> namespacesContribution = new ArrayList<>();

  @Override
  public XPathExpression getAppliedTo() {
    return appliedTo;
  }

  @Override
  public void setAppliedTo(String xpathExpression) {
    checkArgument(xpathExpression != null, "The xpath expression must not be null.");
    try {
      this.appliedTo = XPathFactory.instance().compile(xpathExpression);
      this.originalXpathExpression = xpathExpression;
    } catch (Exception ex) {
      throw new MigrationStepException("The xpath expression must be valid.", ex);
    }
  }

  @Override
  public ApplicationModel getApplicationModel() {
    return applicationModel;
  }

  @Override
  public void setApplicationModel(ApplicationModel applicationModel) {
    this.applicationModel = applicationModel;
  }

  @Override
  public String getDescription() {
    return "";
  }

  @Override
  public List<Namespace> getNamespacesContributions() {
    return namespacesContribution;
  }

  @Override
  public void setNamespacesContributions(List<Namespace> namespaces) {
    this.namespacesContribution = namespaces;
  }

  @Override
  public final void execute(Element element, MigrationReport report) {
    int componentFailures = report.getComponentFailureCount(element);
    try {
      executeMigration(element, report);
      if (reportMetrics() && report.getComponentFailureCount(element) == componentFailures) {
        report.addComponentSuccess(element);
      }
    } catch (Exception e) {
      if (reportMetrics() && report.getComponentFailureCount(element) == componentFailures) {
        report.addComponentFailure(element);
      }
      throw e;
    }
  }

  protected abstract void executeMigration(Element element, MigrationReport report);

  protected boolean reportMetrics() {
    return true;
  }
}
