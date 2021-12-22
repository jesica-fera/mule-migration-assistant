/*
 * Copyright (c) 2020, Mulesoft, LLC. All rights reserved.
 * Use of this source code is governed by a BSD 3-Clause License
 * license that can be found in the LICENSE.txt file.
 */
package com.mulesoft.tools.migration.step;

import static com.google.common.base.Preconditions.checkArgument;
import static com.mulesoft.tools.migration.step.category.MigrationReport.Level.ERROR;
import static java.util.Optional.ofNullable;
import static org.jdom2.Namespace.getNamespace;

import com.mulesoft.tools.migration.exception.MigrationStepException;
import com.mulesoft.tools.migration.project.model.ApplicationModel;
import com.mulesoft.tools.migration.step.category.ApplicationModelContribution;
import com.mulesoft.tools.migration.step.category.MigrationReport;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic unit of execution.
 *
 * @author Mulesoft Inc.
 * @since 1.0.0
 */
public abstract class AbstractApplicationModelMigrationStep implements ApplicationModelContribution {

  private static final Logger logger = LoggerFactory.getLogger(AbstractApplicationModelMigrationStep.class);

  private XPathExpression appliedTo;
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

  /**
   * Wraps the step execution in order to report the element migration result
   *
   *    success:
   *         - report error count not increased OR
   *         - report error count increased same amount as mel/dw errors
   *    failure:
   *         - report error count > mel/dw errors OR
   *         - exception thrown
   */
  @Override
  public final void execute(Element element, MigrationReport report) {
    int entriesBefore = report.getReportEntries(ERROR).size();
    int melFailuresBefore = report.getMelExpressionsFailureCount();
    int dwFailuresBefore = report.getDwTransformsFailureCount();
    try {
      Element sourceElement = null;
      if (element != null) {
        sourceElement = new Element(ofNullable(element.getName()).orElse("test"),
                                    ofNullable(element.getNamespace()).orElse(getNamespace("test")));
      } else {
        sourceElement = new Element("nullElement", getNamespace("nullElement"));
      }
      logger.debug(">>>>> before migrating {}:{} -- step {}", sourceElement.getNamespacePrefix(), sourceElement.getName(),
                   this.getClass().getSimpleName());
      executeMigration(element, report);
      logger.debug(">>>>>  after migrating {}:{} -- step {}", element != null ? element.getNamespacePrefix() : "null",
                   element != null ? element.getName() : null,
                   this.getClass().getSimpleName());
      logger.debug("----- reportMetrics: {} -- failures: report-entries: {} -> {} -- mel {} -> {} -- dw {} -> {}",
                   reportMetrics(), entriesBefore, report.getReportEntries(ERROR).size(),
                   melFailuresBefore, report.getMelExpressionsFailureCount(), dwFailuresBefore,
                   report.getDwTransformsFailureCount());
      if (reportMetrics()) {
        if (report.getReportEntries(ERROR).size() <= entriesBefore + (report.getMelExpressionsFailureCount() - melFailuresBefore)
            + (report.getDwTransformsFailureCount() - dwFailuresBefore)) {
          report.addComponentSuccess(sourceElement);
        } else {
          report.addComponentFailure(sourceElement);
        }
      }
    } catch (Exception e) {
      logger.warn("Exception {} -- migrating {}:{}", e, element != null ? element.getNamespacePrefix() : "null",
                  element != null ? element.getName() : "null");
      if (reportMetrics()) {
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
